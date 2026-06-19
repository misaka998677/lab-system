package com.lab.module.system.service;

import com.lab.module.system.entity.SysDept;
import com.lab.module.system.mapper.SysDeptMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SysDeptService {
    private final SysDeptMapper deptMapper;
    public SysDeptService(SysDeptMapper m) { this.deptMapper = m; }

    public List<SysDept> tree() {
        List<SysDept> all = deptMapper.all();
        return build(all, 0L);
    }

    private List<SysDept> build(List<SysDept> all, Long parentId) {
        return all.stream().filter(d -> Objects.equals(d.getParentId(), parentId)).collect(Collectors.toList());
    }

    public Long create(SysDept d) {
        if (d.getStatus() == null) d.setStatus(1);
        deptMapper.insert(d);
        return d.getId();
    }
    public void update(SysDept d) { deptMapper.update(d); }
    public void delete(Long id)   { deptMapper.deleteById(id); }
    public List<SysDept> all()    { return deptMapper.all(); }
}
