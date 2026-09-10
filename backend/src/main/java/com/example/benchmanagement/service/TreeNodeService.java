package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.CapacityAdjustRequest;
import com.example.benchmanagement.dto.NodeCapacityLogDTO;
import com.example.benchmanagement.dto.TreeNodeDTO;
import com.example.benchmanagement.entity.NodeCapacityLog;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.BenchRepository;
import com.example.benchmanagement.repository.NodeCapacityLogRepository;
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
    private final BenchRepository benchRepository;
    private final NodeCapacityLogRepository capacityLogRepository;

    public List<TreeNodeDTO> getTree() {
        List<TreeNode> allNodes = treeNodeRepository.findAllActiveNodes();
        Map<Long, TreeNodeDTO> nodeMap = new HashMap<>();
        List<TreeNodeDTO> rootNodes = new ArrayList<>();
        List<TreeNodeDTO> pointDtos = new ArrayList<>();

        for (TreeNode node : allNodes) {
            TreeNodeDTO dto = toDTO(node);
            if (node.getLevel() == 3) {
                pointDtos.add(dto);
            }
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

        fillCapacityStatus(pointDtos);

        rootNodes.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
        for (TreeNodeDTO node : nodeMap.values()) {
            node.getChildren().sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
        }

        return rootNodes;
    }

    public List<TreeNodeDTO> getNodesByLevel(Integer level) {
        List<TreeNode> nodes = treeNodeRepository.findByLevelAndIsDeletedFalse(level);
        List<TreeNodeDTO> dtos = nodes.stream().map(this::toDTO).toList();
        if (level == 3) {
            fillCapacityStatus(dtos);
        }
        return dtos;
    }

    public List<TreeNodeDTO> getChildren(Long parentId) {
        List<TreeNode> nodes = treeNodeRepository.findByParentIdAndIsDeletedFalse(parentId);
        List<TreeNodeDTO> dtos = nodes.stream().map(this::toDTO).toList();
        List<TreeNodeDTO> pointDtos = dtos.stream().filter(d -> d.getLevel() == 3).toList();
        if (!pointDtos.isEmpty()) {
            fillCapacityStatus(pointDtos);
        }
        return dtos;
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

        Integer capacity = null;
        if (dto.getLevel() == 3) {
            capacity = dto.getCapacity() != null ? dto.getCapacity() : TreeNode.DEFAULT_CAPACITY;
            if (capacity < 0) {
                throw new IllegalArgumentException("容量必须为大于等于0的整数");
            }
        }

        TreeNode node = TreeNode.builder()
                .parentId(dto.getParentId())
                .level(dto.getLevel())
                .name(dto.getName())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .capacity(capacity)
                .build();

        TreeNode saved = treeNodeRepository.save(node);

        if (saved.getLevel() == 3) {
            NodeCapacityLog logEntry = NodeCapacityLog.builder()
                    .nodeId(saved.getId())
                    .oldCapacity(null)
                    .newCapacity(saved.getCapacity())
                    .occupiedCount(0)
                    .adjustReason("新建点位，初始化容量")
                    .adjustedBy("system")
                    .build();
            capacityLogRepository.save(logEntry);
            saved.setCapacityUpdatedAt(logEntry.getAdjustedAt());
            saved.setCapacityUpdatedReason(logEntry.getAdjustReason());
            treeNodeRepository.save(saved);
        }

        log.info("创建树形节点: id={}, name={}, level={}, capacity={}",
                saved.getId(), saved.getName(), saved.getLevel(), saved.getCapacity());
        return toDTOWithCapacity(saved);
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
        return toDTOWithCapacity(saved);
    }

    /**
     * 调整点位容量，保留调整时间与原因，并校验容量不能小于当前占用数。
     */
    @Transactional
    public TreeNodeDTO adjustCapacity(Long id, CapacityAdjustRequest request) {
        TreeNode node = treeNodeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("点位不存在"));
        if (node.getLevel() != 3) {
            throw new IllegalArgumentException("只有点位(level=3)可以设置容量");
        }

        String reason = request.getAdjustReason() == null ? "" : request.getAdjustReason().trim();
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("调整原因不能为空");
        }

        int newCapacity = request.getCapacity();
        int effectiveCapacity = node.getCapacity() != null ? node.getCapacity() : TreeNode.DEFAULT_CAPACITY;
        if (newCapacity == effectiveCapacity) {
            throw new IllegalArgumentException("新容量与当前容量一致，无需调整");
        }

        long occupied = benchRepository.countByNodeId(id);
        if (newCapacity < occupied) {
            throw new IllegalArgumentException(String.format(
                    "容量调整失败：当前点位已摆放%d张长凳，容量不能低于当前占用数", occupied));
        }

        Integer oldCapacity = node.getCapacity();
        node.setCapacity(newCapacity);
        node.setCapacityUpdatedReason(reason);
        TreeNode saved = treeNodeRepository.save(node);

        NodeCapacityLog logEntry = NodeCapacityLog.builder()
                .nodeId(id)
                .oldCapacity(oldCapacity)
                .newCapacity(newCapacity)
                .occupiedCount((int) occupied)
                .adjustReason(reason)
                .adjustedBy("system")
                .build();
        capacityLogRepository.save(logEntry);

        saved.setCapacityUpdatedAt(logEntry.getAdjustedAt());
        treeNodeRepository.save(saved);

        log.info("调整点位容量: nodeId={}, oldCapacity={}, newCapacity={}, occupied={}",
                id, oldCapacity, newCapacity, occupied);
        return toDTOWithCapacity(saved);
    }

    public List<NodeCapacityLogDTO> getCapacityLogs(Long nodeId) {
        TreeNode node = treeNodeRepository.findByIdAndIsDeletedFalse(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("点位不存在"));
        List<NodeCapacityLog> logs = capacityLogRepository.findByNodeIdOrderByAdjustedAtDesc(nodeId);
        return logs.stream()
                .map(l -> NodeCapacityLogDTO.builder()
                        .id(l.getId())
                        .nodeId(l.getNodeId())
                        .nodeName(node.getName())
                        .oldCapacity(l.getOldCapacity())
                        .newCapacity(l.getNewCapacity())
                        .occupiedCount(l.getOccupiedCount())
                        .adjustReason(l.getAdjustReason())
                        .adjustedAt(l.getAdjustedAt())
                        .adjustedBy(l.getAdjustedBy())
                        .build())
                .toList();
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

        if (node.getLevel() == 3) {
            long occupied = benchRepository.countByNodeId(id);
            if (occupied > 0) {
                throw new IllegalArgumentException(String.format(
                        "该点位下仍有%d张长凳，无法删除", occupied));
            }
        }

        node.setIsDeleted(true);
        treeNodeRepository.save(node);
        log.info("删除树形节点: id={}, name={}", id, node.getName());
    }

    public TreeNodeDTO getNodeById(Long id) {
        TreeNode node = treeNodeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在"));
        return toDTOWithCapacity(node);
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

    /**
     * 批量填充点位的占用数、剩余数与有效容量。
     */
    private void fillCapacityStatus(List<TreeNodeDTO> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        List<Long> pointIds = points.stream().map(TreeNodeDTO::getId).toList();
        Map<Long, Long> countMap = new HashMap<>();
        for (Object[] row : benchRepository.countByNodeIds(pointIds)) {
            countMap.put((Long) row[0], (Long) row[1]);
        }
        for (TreeNodeDTO point : points) {
            int capacity = point.getCapacity() != null ? point.getCapacity() : TreeNode.DEFAULT_CAPACITY;
            point.setCapacity(capacity);
            long occupied = countMap.getOrDefault(point.getId(), 0L);
            point.setOccupiedCount(occupied);
            point.setRemainingCount(Math.max(0, capacity - occupied));
        }
    }

    private TreeNodeDTO toDTO(TreeNode node) {
        return TreeNodeDTO.builder()
                .id(node.getId())
                .parentId(node.getParentId())
                .level(node.getLevel())
                .name(node.getName())
                .sortOrder(node.getSortOrder())
                .capacity(node.getCapacity())
                .capacityUpdatedAt(node.getCapacityUpdatedAt())
                .capacityUpdatedReason(node.getCapacityUpdatedReason())
                .children(new ArrayList<>())
                .build();
    }

    private TreeNodeDTO toDTOWithCapacity(TreeNode node) {
        TreeNodeDTO dto = toDTO(node);
        if (node.getLevel() == 3) {
            fillCapacityStatus(List.of(dto));
        }
        return dto;
    }
}
