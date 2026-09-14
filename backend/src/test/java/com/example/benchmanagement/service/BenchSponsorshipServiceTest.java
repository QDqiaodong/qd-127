package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.BenchSponsorshipDTO;
import com.example.benchmanagement.dto.BenchSponsorshipRequest;
import com.example.benchmanagement.entity.BenchSponsorship;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.BenchSponsorshipRepository;
import com.example.benchmanagement.repository.TreeNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BenchSponsorshipServiceTest {

    @Mock
    private BenchSponsorshipRepository sponsorshipRepository;
    @Mock
    private TreeNodeRepository treeNodeRepository;

    @InjectMocks
    private BenchSponsorshipService sponsorshipService;

    private TreeNode district;
    private TreeNode section;
    private TreeNode point;

    @BeforeEach
    void setUp() {
        district = TreeNode.builder().id(10L).level(1).name("A街区").build();
        section = TreeNode.builder().id(20L).parentId(10L).level(2).name("主干道").build();
        point = TreeNode.builder().id(30L).parentId(20L).level(3).name("广场前").build();
    }

    @Test
    void create_pointNotLevel3_throws() {
        BenchSponsorshipRequest request = BenchSponsorshipRequest.builder()
                .pointId(20L)
                .merchantName("某商户")
                .sponsorshipText("文案")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .build();
        when(treeNodeRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(section));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sponsorshipService.createSponsorship(request));
        assertTrue(ex.getMessage().contains("点位"));
        verify(sponsorshipRepository, never()).save(any());
    }

    @Test
    void create_endBeforeStart_throws() {
        BenchSponsorshipRequest request = BenchSponsorshipRequest.builder()
                .pointId(30L)
                .merchantName("某商户")
                .sponsorshipText("文案")
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now())
                .build();
        when(treeNodeRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(point));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sponsorshipService.createSponsorship(request));
        assertTrue(ex.getMessage().contains("结束日期"));
    }

    @Test
    void create_futureStart_isPendingAndTreeNamesFilled() {
        LocalDate today = LocalDate.now();
        BenchSponsorshipRequest request = BenchSponsorshipRequest.builder()
                .pointId(30L)
                .merchantName("  张三奶茶  ")
                .sponsorshipText("张三奶茶请你歇脚")
                .startDate(today.plusDays(3))
                .endDate(today.plusDays(30))
                .build();
        when(treeNodeRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(point));
        when(treeNodeRepository.findAllActiveNodes()).thenReturn(List.of(district, section, point));
        when(sponsorshipRepository.save(any(BenchSponsorship.class))).thenAnswer(invocation -> {
            BenchSponsorship saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        BenchSponsorshipDTO dto = sponsorshipService.createSponsorship(request);

        assertEquals(1L, dto.getId());
        assertEquals("张三奶茶", dto.getMerchantName());
        assertEquals("广场前", dto.getPointName());
        assertEquals("主干道", dto.getSectionName());
        assertEquals("A街区", dto.getDistrictName());
        assertEquals(BenchSponsorshipService.EFFECTIVE_STATUS_PENDING, dto.getEffectiveStatus());
        assertFalse(dto.getExpired());
    }

    @Test
    void list_statusDerivedFromDates_pendingActiveExpired() {
        LocalDate today = LocalDate.now();
        BenchSponsorship pending = BenchSponsorship.builder().id(1L).pointId(30L)
                .merchantName("待生效商户").sponsorshipText("待")
                .startDate(today.plusDays(1)).endDate(today.plusDays(10)).build();
        BenchSponsorship active = BenchSponsorship.builder().id(2L).pointId(30L)
                .merchantName("生效商户").sponsorshipText("中")
                .startDate(today.minusDays(1)).endDate(today.plusDays(10)).build();
        BenchSponsorship expired = BenchSponsorship.builder().id(3L).pointId(30L)
                .merchantName("过期商户").sponsorshipText("过")
                .startDate(today.minusDays(20)).endDate(today.minusDays(1)).build();
        when(sponsorshipRepository.findAllByOrderByCreatedAtDescIdDesc())
                .thenReturn(List.of(pending, active, expired));
        when(treeNodeRepository.findAllActiveNodes()).thenReturn(List.of(district, section, point));

        List<BenchSponsorshipDTO> all = sponsorshipService.listSponsorships(null, null, null, null);

        assertEquals(BenchSponsorshipService.EFFECTIVE_STATUS_PENDING, all.get(0).getEffectiveStatus());
        assertEquals(BenchSponsorshipService.EFFECTIVE_STATUS_ACTIVE, all.get(1).getEffectiveStatus());
        assertEquals(BenchSponsorshipService.EFFECTIVE_STATUS_EXPIRED, all.get(2).getEffectiveStatus());
        assertTrue(all.get(2).getExpired());

        List<BenchSponsorshipDTO> expiredOnly =
                sponsorshipService.listSponsorships(null, null, null, BenchSponsorshipService.EFFECTIVE_STATUS_EXPIRED);
        assertEquals(1, expiredOnly.size());
        assertEquals("过期商户", expiredOnly.get(0).getMerchantName());
    }

    @Test
    void list_expiringSoon_markedAndFiltered() {
        LocalDate today = LocalDate.now();
        BenchSponsorship expiringToday = BenchSponsorship.builder().id(1L).pointId(30L)
                .merchantName("今日到期商户").sponsorshipText("今")
                .startDate(today.minusDays(10)).endDate(today).build();
        BenchSponsorship expiringInThreeDays = BenchSponsorship.builder().id(2L).pointId(30L)
                .merchantName("三天后到期商户").sponsorshipText("三")
                .startDate(today.minusDays(10)).endDate(today.plusDays(3)).build();
        BenchSponsorship activeLater = BenchSponsorship.builder().id(3L).pointId(30L)
                .merchantName("正常生效商户").sponsorshipText("正")
                .startDate(today.minusDays(10)).endDate(today.plusDays(10)).build();
        BenchSponsorship pendingSoon = BenchSponsorship.builder().id(4L).pointId(30L)
                .merchantName("待生效商户").sponsorshipText("待")
                .startDate(today.plusDays(1)).endDate(today.plusDays(3)).build();
        when(sponsorshipRepository.findAllByOrderByCreatedAtDescIdDesc())
                .thenReturn(List.of(expiringToday, expiringInThreeDays, activeLater, pendingSoon));
        when(treeNodeRepository.findAllActiveNodes()).thenReturn(List.of(district, section, point));

        List<BenchSponsorshipDTO> all = sponsorshipService.listSponsorships(null, null, null, null);

        assertTrue(all.get(0).getExpiringSoon());
        assertEquals(BenchSponsorshipService.EFFECTIVE_STATUS_EXPIRING_SOON, all.get(0).getEffectiveStatus());
        assertEquals(0L, all.get(0).getDaysRemaining());
        assertTrue(all.get(1).getExpiringSoon());
        assertEquals(BenchSponsorshipService.EFFECTIVE_STATUS_EXPIRING_SOON, all.get(1).getEffectiveStatus());
        assertEquals(3L, all.get(1).getDaysRemaining());
        assertFalse(all.get(2).getExpiringSoon());
        assertEquals(10L, all.get(2).getDaysRemaining());
        assertFalse(all.get(3).getExpiringSoon());

        List<BenchSponsorshipDTO> expiringOnly =
                sponsorshipService.listSponsorships(null, null, null, BenchSponsorshipService.EFFECTIVE_STATUS_EXPIRING_SOON);
        assertEquals(2, expiringOnly.size());
        assertEquals("今日到期商户", expiringOnly.get(0).getMerchantName());
    }

    @Test
    void getExpiringSoonSponsorships_usesSameDateWindow() {
        LocalDate today = LocalDate.now();
        when(sponsorshipRepository.findExpiringSoonByPointIds(
                List.of(30L), today, today.plusDays(3)))
                .thenReturn(List.of(BenchSponsorship.builder().id(1L).pointId(30L).build()));

        List<BenchSponsorship> result =
                sponsorshipService.getExpiringSoonSponsorships(List.of(30L), today);

        assertEquals(1, result.size());
        verify(sponsorshipRepository).findExpiringSoonByPointIds(
                List.of(30L), today, today.plusDays(3));
    }

    @Test
    void list_invalidStatus_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sponsorshipService.listSponsorships(null, null, null, 5));
        assertTrue(ex.getMessage().contains("即将到期"));
    }

    @Test
    void list_filterByPoint_returnsOnlyThatPoint() {
        LocalDate today = LocalDate.now();
        BenchSponsorship one = BenchSponsorship.builder().id(1L).pointId(30L)
                .merchantName("A").sponsorshipText("a")
                .startDate(today).endDate(today.plusDays(5)).build();
        BenchSponsorship other = BenchSponsorship.builder().id(2L).pointId(31L)
                .merchantName("B").sponsorshipText("b")
                .startDate(today).endDate(today.plusDays(5)).build();
        when(sponsorshipRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(one, other));
        when(treeNodeRepository.findAllActiveNodes()).thenReturn(List.of(district, section, point));

        List<BenchSponsorshipDTO> result =
                sponsorshipService.listSponsorships(null, null, 30L, null);

        assertEquals(1, result.size());
        assertEquals(30L, result.get(0).getPointId());
    }
}
