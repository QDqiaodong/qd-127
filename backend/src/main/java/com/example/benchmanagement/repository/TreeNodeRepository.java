package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.TreeNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TreeNodeRepository extends JpaRepository<TreeNode, Long> {

    List<TreeNode> findByParentIdAndIsDeletedFalse(Long parentId);

    List<TreeNode> findByLevelAndIsDeletedFalse(Integer level);

    List<TreeNode> findByParentIdInAndIsDeletedFalse(List<Long> parentIds);

    @Query("SELECT t FROM TreeNode t WHERE t.isDeleted = false ORDER BY t.level ASC, t.sortOrder ASC")
    List<TreeNode> findAllActiveNodes();

    @Query("SELECT t FROM TreeNode t WHERE t.id IN :ids AND t.isDeleted = false")
    List<TreeNode> findByIdsAndIsDeletedFalse(List<Long> ids);

    boolean existsByParentIdAndNameAndIsDeletedFalse(Long parentId, String name);

    @Query("SELECT COUNT(t) FROM TreeNode t WHERE t.parentId = :parentId AND t.isDeleted = false")
    long countByParentId(Long parentId);

    Optional<TreeNode> findByIdAndIsDeletedFalse(Long id);
}
