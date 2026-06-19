package com.lab.module.system.service;

import com.lab.module.system.entity.SysMenu;
import com.lab.module.system.mapper.SysMenuMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SysMenuService {
    private final SysMenuMapper menuMapper;
    public SysMenuService(SysMenuMapper m) { this.menuMapper = m; }

    public List<SysMenu> tree() { return build(menuMapper.all(), 0L); }

    public Long create(SysMenu m) {
        if (m.getVisible() == null) m.setVisible(1);
        menuMapper.insert(m);
        return m.getId();
    }
    public void update(SysMenu m) { menuMapper.update(m); }
    public void delete(Long id)   { menuMapper.deleteById(id); }

    private List<SysMenu> build(List<SysMenu> all, Long parentId) {
        return all.stream()
                .filter(m -> Objects.equals(m.getParentId(), parentId))
                .peek(m -> m.setChildren(build(all, m.getId())))
                .collect(Collectors.toList());
    }
}
