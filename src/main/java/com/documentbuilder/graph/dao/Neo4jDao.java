package com.documentbuilder.graph.dao;

import com.documentbuilder.constants.Constants;
import com.documentbuilder.graph.EmbeddedNeo4jManager;
import com.documentbuilder.graph.dto.ClassNode;
import com.documentbuilder.graph.dto.FieldNode;
import com.documentbuilder.graph.dto.MethodNode;
import com.documentbuilder.graph.dto.PackageNode;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.Schema;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Neo4jDao {

    private final EmbeddedNeo4jManager manager;

    public Neo4jDao() {
        this.manager = EmbeddedNeo4jManager.getInstance();
    }

    public Node createNode(String label, Map<String, Object> properties, Transaction tx) {
            Node node = tx.createNode(Label.label(label));
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                node.setProperty(entry.getKey(), entry.getValue());
            }
            return node;

    }

    public Relationship createRelationship(long fromNodeId, long toNodeId, String relType, Map<String, Object> properties, Transaction tx) {

            Node fromNode = tx.getNodeById(fromNodeId);
            Node toNode = tx.getNodeById(toNodeId);
            Relationship relationship = fromNode.createRelationshipTo(toNode, RelationshipType.withName(relType));
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                relationship.setProperty(entry.getKey(), entry.getValue());
            }

            return relationship;

    }

    public void updateNodeProperties(long nodeId, Map<String, Object> updates) {
        try (Transaction tx = this.manager.getConnection().beginTx()) {
            Node node = tx.getNodeById(nodeId);
            updates.forEach(node::setProperty);
            tx.commit();
        }
    }

    public Node findNodeById(long id) {
        try (Transaction tx = this.manager.getConnection().beginTx()) {
            Node node = tx.getNodeById(id);  // CORRECT: use tx, not db
            tx.commit();
            return node;
        } catch (NotFoundException e) {
            return null;
        }
    }

    public List<Node> findNodesByLabelAndProperty(String label, String key, Object value, Transaction tx) {
        List<Node> result = new ArrayList<>();
        ResourceIterator<Node> nodes = tx.findNodes(Label.label(label), key, value);
        while (nodes.hasNext()) {
            result.add(nodes.next());
        }
        return result;
    }

    public void deleteNode(long nodeId) {
        try (Transaction tx = this.manager.getConnection().beginTx()) {
            Node node = tx.getNodeById(nodeId);
            for (Relationship rel : node.getRelationships()) {
                rel.delete();
            }
            node.delete();
            tx.commit();
        }
    }

    public Relationship findRelationshipById(long relId) {
        try (Transaction tx = this.manager.getConnection().beginTx()) {
            Relationship rel = tx.getRelationshipById(relId);
            tx.commit();
            return rel;
        } catch (NotFoundException e) {
            return null;
        }
    }

    public void updateRelationshipProperties(long relId, Map<String, Object> updates) {
        try (Transaction tx = this.manager.getConnection().beginTx()) {
            Relationship rel = tx.getRelationshipById(relId);
            updates.forEach(rel::setProperty);
            tx.commit();
        }
    }

    public void deleteRelationship(long relId) {
        try (Transaction tx = this.manager.getConnection().beginTx()) {
            Relationship rel = tx.getRelationshipById(relId);
            rel.delete();
            tx.commit();
        }
    }

    public Result executeCypher(String query, Map<String, Object> params) {
        try (Transaction tx = this.manager.getConnection().beginTx()) {
            Result result = tx.execute(query, params);
            tx.commit();
            return result;
        }
    }

    public void createIndex(String label, String propertyKey) {
        try (Transaction tx = this.manager.getConnection().beginTx()) {
            Schema schema = tx.schema();
            schema.indexFor(Label.label(label)).on(propertyKey).create();
            tx.commit();
        }
    }

    public void awaitIndexesOnline() {
        try (Transaction tx = this.manager.getConnection().beginTx()) {
            tx.schema().awaitIndexesOnline(10, TimeUnit.SECONDS);
            tx.commit();
        }
    }

    public Node createClass(ClassNode classDto) {
        Node classNode =  null;
        Node packageNode = null;
        Map<String, Object> classProps;
        Map<String, Object> packageProps;
        Map<String, Object> methodProps;

        try (Transaction tx = this.manager.getConnection().beginTx()) {
            packageNode = creatPackage(classDto, tx);

            classDto.getPackageNode().setId(packageNode.getId());

            classProps = getClassProperties(classDto);
            classNode = createNode(Constants.CLASS_LABEL, classProps,tx);
            classDto.setId(classNode.getId());
            linkHasClass(classDto,classDto.getPackageNode(),tx);
            for(MethodNode method : classDto.getMethods()) {
                method.setClassId(classDto.getId());
                createMethod(method,tx);
                linkHasMethod(classDto,method,tx);
            }

            for(FieldNode field : classDto.getFields()) {
                field.setClassId(classDto.getId());
                createField(field,tx);
                linkHasField(classDto,field,tx);
            }
            tx.commit();
        }

        return classNode;
    }

    private Node creatPackage(ClassNode classDto, Transaction tx) {
        Map<String, Object> packageProps;
        Node packageNode = null;

        List<Node> existingPackages = findNodesByLabelAndProperty(Constants.PACKAGE_LABEL, Constants.NAME_KEY, classDto.getPackageNode().getName(), tx);
        for(Node node: existingPackages) {
            packageNode = node;
        }
        if (packageNode == null ) {
            packageProps = new HashMap<>();
            packageProps.put(Constants.NAME_KEY, classDto.getPackageNode().getName());
            packageNode = createNode(Constants.PACKAGE_LABEL, packageProps, tx);
        }
        return packageNode;
    }

    private Map<String, Object> getClassProperties(ClassNode classDto) {
        Map<String, Object> classProps = new HashMap<>();
        classProps.put(Constants.NAME_KEY, classDto.getName());
        classProps.put(Constants.PACKAGE_NAME_KEY, classDto.getPackageNode());
        classProps.put(Constants.IS_ABSTRACT_KEY, classDto.isAbstract());
        classProps.put(Constants.IS_INTERFACE_KEY, classDto.isInterface());
        classProps.put(Constants.JAVADOC_KEY, classDto.getJavadoc());
        classProps.put(Constants.MODIFIERS_KEY, classDto.getModifiers().toArray(new String[0]));
        classProps.put(Constants.PACKAGE_ID_KEY, classDto.getPackageNode().getId());
        return classProps;
    }

    public Node createMethod(MethodNode methodDto, Transaction tx) {
        Map<String, Object> props = getMethodProps(methodDto);
        Node node = createNode(Constants.METHOD_LABEL, props,tx);
        methodDto.setId(node.getId());
        return node;
    }

    private static Map<String, Object> getMethodProps(MethodNode methodDto) {
        Map<String, Object> props = new HashMap<>();
        props.put(Constants.NAME_KEY, methodDto.getName());
        props.put(Constants.RETURN_TYPE_KEY, methodDto.getReturnType());
        props.put(Constants.MODIFIERS_KEY, methodDto.getModifiers().toArray(new String[0]));
        props.put(Constants.JAVADOC_KEY, methodDto.getJavadoc());
        props.put(Constants.CLASS_ID_KEY, methodDto.getClassId());
        props.put(Constants.PARAMETERS_KEY, methodDto.getParameters());
        return props;
    }

    public void linkHasMethod(ClassNode classDto, MethodNode methodDto, Transaction tx) {
        createRelationship(classDto.getId(), methodDto.getId(), "HAS_METHOD", null, tx);
    }

    public void linkIsARelationship(ClassNode child, ClassNode parent, Transaction tx) {
        createRelationship(child.getId(), parent.getId(), "IS_A", null, tx);

    }

    public void linkHasField(ClassNode classDto, FieldNode fieldDto, Transaction tx) {
        createRelationship(classDto.getId(), fieldDto.getId(), "HAS_FIELD", null,tx);
    }

    public void linkHasClass(ClassNode classDto, PackageNode packageDto, Transaction tx) {
        createRelationship(packageDto.getId(),classDto.getId() , "HAS_CLASS", null,tx);
    }

    public Node createField(FieldNode fieldDto, Transaction tx) {
        Map<String, Object> props = getFieldProperties(fieldDto);
        Node node = createNode(Constants.FIELD_LABEL, props,tx);
        fieldDto.setId(node.getId());
        return node;
    }

    private static Map<String, Object> getFieldProperties(FieldNode fieldDto) {
        Map<String, Object> props = new HashMap<>();
        props.put(Constants.NAME_KEY, fieldDto.getName());
        props.put(Constants.MODIFIERS_KEY, fieldDto.getModifiers().toArray(new String[0]));
        props.put(Constants.JAVADOC_KEY, fieldDto.getJavadoc());
        props.put(Constants.CLASS_ID_KEY, fieldDto.getClassId());
        props.put(Constants.TYPE_KEY, fieldDto.getType());
        return props;
    }

    public void shutdown() {
        this.manager.shutdown();
    }
}

