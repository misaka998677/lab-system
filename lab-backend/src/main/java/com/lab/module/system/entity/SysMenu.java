package com.lab.module.system.entity;

import lombok.Data;
import java.util.List;

@Data
public class SysMenu {
    private Long   id;
    private Long   parentId;
    private String name;
    private String path;
    private String component;
    private String icon;
    private String perm;
    private Integer type;
    private Integer sortNo;
    private Integer visible;

    /** 前端用的子菜单 */
    private List<SysMenu> children;
}
