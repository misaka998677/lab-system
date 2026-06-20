package com.lab.module.system.mapper;

import com.lab.module.system.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserMapper {
    SysUser findByUsername(@Param("username") String username);

    SysUser findById(@Param("id") Long id);

    List<SysUser> page(@Param("keyword") String keyword,
                       @Param("deptId") Long deptId,
                       @Param("status") Integer status);

    int insert(SysUser u);
    int update(SysUser u);
    int deleteById(@Param("id") Long id);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int updatePassword(@Param("id") Long id, @Param("password") String password);
    int updateFailInfo(@Param("id") Long id,
                       @Param("failCount") Integer failCount,
                       @Param("firstFailAt") java.time.LocalDateTime firstFailAt,
                       @Param("lockedUntil") java.time.LocalDateTime lockedUntil);

    List<String> findRoleCodes(@Param("userId") Long userId);
    List<String> findPermsByUserId(@Param("userId") Long userId);
    List<Long>   findRoleIdsByUserId(@Param("userId") Long userId);

    Long findDeptIdByUserId(@Param("userId") Long userId);
    List<Long> findManagedLabIds(@Param("userId") Long userId);
    List<Long> findLabIdsByDeptId(@Param("deptId") Long deptId);

    int deleteUserRoles(@Param("userId") Long userId);
    int insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    List<SysUser> findAll();
}
