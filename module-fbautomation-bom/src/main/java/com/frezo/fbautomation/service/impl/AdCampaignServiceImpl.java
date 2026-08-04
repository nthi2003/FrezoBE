package com.frezo.fbautomation.service.impl;

import com.frezo.fbautomation.dto.request.AdCampaignRequest;
import com.frezo.fbautomation.dto.response.AdCampaignResponse;
import com.frezo.fbautomation.entity.AdCampaign;
import com.frezo.fbautomation.repository.AdCampaignRepository;
import com.frezo.fbautomation.service.AdCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdCampaignServiceImpl implements AdCampaignService {

    private final AdCampaignRepository repository;

    @Override
    public List<AdCampaignResponse> list(String platform, String status) {
        return repository.findAll().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .filter(c -> platform == null || platform.isBlank() || platform.equalsIgnoreCase(c.getPlatform()))
                .filter(c -> status == null || status.isBlank() || status.equalsIgnoreCase(c.getStatus()))
                .sorted(Comparator.comparing(AdCampaign::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AdCampaignResponse get(String id) {
        return toResponse(mustFind(id));
    }

    @Override
    @Transactional
    public AdCampaignResponse create(AdCampaignRequest req) {
        AdCampaign c = AdCampaign.builder()
                .name(req.getName().trim())
                .platform(nz(req.getPlatform(), "FACEBOOK"))
                .objective(req.getObjective())
                .status(nz(req.getStatus(), "DRAFT"))
                .budget(nzDec(req.getBudget()))
                .spend(nzDec(req.getSpend()))
                .impressions(nzLong(req.getImpressions()))
                .clicks(nzLong(req.getClicks()))
                .leads(nzLong(req.getLeads()))
                .revenue(nzDec(req.getRevenue()))
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .externalAdId(req.getExternalAdId())
                .landingUrl(req.getLandingUrl())
                .note(req.getNote())
                .build();
        return toResponse(repository.save(c));
    }

    @Override
    @Transactional
    public AdCampaignResponse update(String id, AdCampaignRequest req) {
        AdCampaign c = mustFind(id);
        c.setName(req.getName().trim());
        if (req.getPlatform() != null) c.setPlatform(req.getPlatform());
        c.setObjective(req.getObjective());
        if (req.getStatus() != null) c.setStatus(req.getStatus());
        if (req.getBudget() != null) c.setBudget(req.getBudget());
        if (req.getSpend() != null) c.setSpend(req.getSpend());
        if (req.getImpressions() != null) c.setImpressions(req.getImpressions());
        if (req.getClicks() != null) c.setClicks(req.getClicks());
        if (req.getLeads() != null) c.setLeads(req.getLeads());
        if (req.getRevenue() != null) c.setRevenue(req.getRevenue());
        c.setStartDate(req.getStartDate());
        c.setEndDate(req.getEndDate());
        c.setExternalAdId(req.getExternalAdId());
        c.setLandingUrl(req.getLandingUrl());
        c.setNote(req.getNote());
        return toResponse(repository.save(c));
    }

    @Override
    @Transactional
    public void delete(String id) {
        AdCampaign c = mustFind(id);
        c.setIsDeleted(true);
        repository.save(c);
    }

    @Override
    public Map<String, Object> dashboard() {
        List<AdCampaign> all = repository.findAll().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .toList();
        BigDecimal spend = all.stream().map(c -> nzDec(c.getSpend())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal budget = all.stream().map(c -> nzDec(c.getBudget())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal revenue = all.stream().map(c -> nzDec(c.getRevenue())).reduce(BigDecimal.ZERO, BigDecimal::add);
        long impressions = all.stream().mapToLong(c -> nzLong(c.getImpressions())).sum();
        long clicks = all.stream().mapToLong(c -> nzLong(c.getClicks())).sum();
        long leads = all.stream().mapToLong(c -> nzLong(c.getLeads())).sum();

        Map<String, Object> m = new HashMap<>();
        m.put("totalCampaigns", all.size());
        m.put("activeCampaigns", all.stream().filter(c -> "ACTIVE".equals(c.getStatus())).count());
        m.put("totalBudget", budget);
        m.put("totalSpend", spend);
        m.put("totalImpressions", impressions);
        m.put("totalClicks", clicks);
        m.put("totalLeads", leads);
        m.put("totalRevenue", revenue);
        m.put("ctr", ratio(clicks, impressions));
        m.put("cpc", clicks > 0 ? spend.divide(BigDecimal.valueOf(clicks), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        m.put("cpl", leads > 0 ? spend.divide(BigDecimal.valueOf(leads), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        m.put("roas", spend.signum() > 0 ? revenue.divide(spend, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        m.put("byPlatform", all.stream().collect(Collectors.groupingBy(
                c -> c.getPlatform() == null ? "OTHER" : c.getPlatform(),
                Collectors.counting())));
        return m;
    }

    private AdCampaign mustFind(String id) {
        AdCampaign c = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chiến dịch Ads"));
        if (Boolean.TRUE.equals(c.getIsDeleted())) {
            throw new IllegalArgumentException("Chiến dịch đã xoá");
        }
        return c;
    }

    private AdCampaignResponse toResponse(AdCampaign c) {
        AdCampaignResponse r = new AdCampaignResponse();
        r.setId(c.getId());
        r.setName(c.getName());
        r.setPlatform(c.getPlatform());
        r.setObjective(c.getObjective());
        r.setStatus(c.getStatus());
        r.setBudget(nzDec(c.getBudget()));
        r.setSpend(nzDec(c.getSpend()));
        r.setImpressions(nzLong(c.getImpressions()));
        r.setClicks(nzLong(c.getClicks()));
        r.setLeads(nzLong(c.getLeads()));
        r.setRevenue(nzDec(c.getRevenue()));
        r.setStartDate(c.getStartDate());
        r.setEndDate(c.getEndDate());
        r.setExternalAdId(c.getExternalAdId());
        r.setLandingUrl(c.getLandingUrl());
        r.setNote(c.getNote());
        r.setCreatedDate(c.getCreatedDate());
        long imps = nzLong(c.getImpressions());
        long clk = nzLong(c.getClicks());
        long ld = nzLong(c.getLeads());
        BigDecimal spend = nzDec(c.getSpend());
        BigDecimal rev = nzDec(c.getRevenue());
        r.setCtr(ratio(clk, imps));
        r.setCpc(clk > 0 ? spend.divide(BigDecimal.valueOf(clk), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        r.setCpl(ld > 0 ? spend.divide(BigDecimal.valueOf(ld), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        r.setRoas(spend.signum() > 0 ? rev.divide(spend, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        return r;
    }

    private static BigDecimal ratio(long num, long den) {
        if (den <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(num).divide(BigDecimal.valueOf(den), 4, RoundingMode.HALF_UP);
    }

    private static String nz(String v, String def) {
        return v == null || v.isBlank() ? def : v.trim();
    }

    private static BigDecimal nzDec(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static long nzLong(Long v) {
        return v == null ? 0L : v;
    }
}
