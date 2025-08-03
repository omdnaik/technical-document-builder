package com.documentbuilder.graph.dto;

import java.util.List;

public class ClassNode {
    private Long id;

    private String name;
    private PackageNode packageNode;
    private List<MethodNode> methods;
    private List<FieldNode> fields;
    private List<String> modifiers;

    private boolean isInterface;
    private boolean isAbstract;
    private String javadoc;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PackageNode getPackageNode() {
        return packageNode;
    }

    public void setPackageNode(PackageNode packageNode) {
        this.packageNode = packageNode;
    }

    public List<MethodNode> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodNode> methods) {
        this.methods = methods;
    }

    public List<FieldNode> getFields() {
        return fields;
    }

    public void setFields(List<FieldNode> fields) {
        this.fields = fields;
    }

    public List<String> getModifiers() {
        return modifiers;
    }

    public void setModifiers(List<String> modifiers) {
        this.modifiers = modifiers;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public void setInterface(boolean anInterface) {
        isInterface = anInterface;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean anAbstract) {
        isAbstract = anAbstract;
    }

    public String getJavadoc() {
        return javadoc;
    }

    public void setJavadoc(String javadoc) {
        this.javadoc = javadoc;
    }
}
