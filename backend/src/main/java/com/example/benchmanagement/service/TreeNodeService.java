package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.TreeNodeDTO;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.TreeNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TreeNodeService {

    private final TreeNodeRepository treeNodeRepository;

    public List<TreeNodeDTO> getTree() {
        List<TreeNode> allNodes = treeNodeRepository.findAllActiveNodes();
        Map<Long, TreeNodeDTO> nodeMap = new HashMap<>();
        List<TreeNodeDTO> rootNodes = new ArrayList<>();

        for (TreeNode node : allNodes) {
            TreeNodeDTO dto = TreeNodeDTO.builder()
                    .id(node.getId())
                    .parentId(node.getParentId())
                    .level(node.getLevel())
                    .name(node.getName())
                    .sortOrder(node.getSortOrder())
                    .children(new ArrayList<>())
                    .build();
            nodeMap.put(node.getId(), dto);

            if (node.getParentId() == null) {
                rootNodes.add(dto);
            } else {
                TreeNodeDTO parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(dto);
                }
            }
        }

        rootNodes.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
        for (TreeNodeDTO node : nodeMap.values()) {
            node.getChildren().sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
        }

        return rootNodes;
    }

    public List<TreeNodeDTO> getNodesByLevel(Integer level) {
        List<TreeNode> nodes = treeNodeRepository.findByLevelAndIsDeletedFalse(level);
        return nodes.stream()
                .map(this::toDTO)
                .toList();
    }

    public List<TreeNodeDTO> getChildren(Long parentId) {
        List<TreeNode> nodes = treeNodeRepository.findByParentIdAndIsDeletedFalse(parentId);
        return nodes.stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public TreeNodeDTO createNode(TreeNodeDTO dto) {
        validateNodeLevel(dto.getParentId(), dto.getLevel());

        if (dto.getParentId() != null) {
            TreeNode parent = treeNodeRepository.findByIdAndIsDeletedFalse(dto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("父节点不存在"));
            if (parent.getLevel() >= 3) {
                throw new IllegalArgumentException("点位下不能再创建子节点");
            }
        }

        if (treeNodeRepository.existsByParentIdAndNameAndIsDeletedFalse(dto.getParentId(), dto.getName())) {
            throw new IllegalArgumentException("同级节点名称已存在");
        }

        TreeNode node = TreeNode.builder()
                .parentId(dto.getParentId())
                .level(dto.getLevel())
                .name(dto.getName())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .build();

        TreeNode saved = treeNodeRepository.save(node);
        log.info("创建树形节点: id={}, name={}, level={}", saved.getId(), saved.getName(), saved.getLevel());
        return toDTO(saved);
    }

    @Transactional
    public TreeNodeDTO updateNode(Long id, TreeNodeDTO dto) {
        TreeNode node = treeNodeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在"));

        if (dto.getName() != null && !dto.getName().equals(node.getName())) {
            if (treeNodeRepository.existsByParentIdAndNameAndIsDeletedFalse(node.getParentId(), dto.getName())) {
                throw new IllegalArgumentException("同级节点名称已存在");
            }
            node.setName(dto.getName());
        }

        if (dto.getSortOrder() != null) {
            node.setSortOrder(dto.getSortOrder());
        }

        TreeNode saved = treeNodeRepository.save(node);
        log.info("更新树形节点: id={}, name={}", saved.getId(), saved.getName());
        return toDTO(saved);
    }

    @Transactional
    public void deleteNode(Long id) {
        TreeNode node = treeNodeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在"));

        if (node.getLevel() < 3) {
            long childCount = treeNodeRepository.countByParentId(id);
            if (childCount > 0) {
                throw new IllegalArgumentException("该节点下存在子节点，无法删除");
            }
        }

        node.setIsDeleted(true);
        treeNodeRepository.save(node);
        log.info("删除树形节点: id={}, name={}", id, node.getName());
    }

    public TreeNodeDTO getNodeById(Long id) {
        TreeNode node = treeNodeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在"));
        return toDTO(node);
    }

    private void validateNodeLevel(Long parentId, Integer level) {
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("层级必须为1、2或3");
        }

        if (parentId == null) {
            if (level != 1) {
                throw new IllegalArgumentException("根节点层级必须为1");
            }
        } else {
            TreeNode parent = treeNodeRepository.findByIdAndIsDeletedFalse(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("父节点不存在"));
            if (level != parent.getLevel() + 1) {
                throw new IllegalArgumentException("子节点层级必须为父节点层级+1");
            }
        }
    }

    private TreeNodeDTO toDTO(TreeNode node) {
        return TreeNodeDTO.builder()
                .id(node.getId())
                .parentId(node.getParentId())
                .level(node.getLevel())
                .name(node.getName())
                .sortOrder(node.getSortOrder())
                .children(new ArrayList<>())
                .build();
    }
}
