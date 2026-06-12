package com.horseracing.dto;

public class PermissionResponse {
    private Integer permissionId;
    private String permissionName;
    private String description;

    public PermissionResponse(Integer permissionId, String permissionName, String description) {
        this.permissionId = permissionId;
        this.permissionName = permissionName;
        this.description = description;
    }

    public Integer getPermissionId() {
        return permissionId;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public String getDescription() {
        return description;
    }
}
