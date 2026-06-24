package com.frezo.qtht.service;

import com.frezo.product.repository.ProductRepository;
import com.frezo.product.repository.SaleOrderRepository;
import com.frezo.qtht.dto.response.DashboardSummaryResponse;
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.warehouse.repository.StockBalanceRepository;
import com.frezo.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl extends DashboardService {

    private final PersonRepository personRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final ProductRepository productRepository;

    @Override
    public DashboardSummaryResponse getSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfLastMonth = today.minusMonths(1).withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfLastMonth = today.withDayOfMonth(1).atStartOfDay().minusNanos(1);
        LocalDateTime startOfNextMonth = today.with(TemporalAdjusters.firstDayOfNextMonth()).atStartOfDay();

        long ordersToday = saleOrderRepository.countByCreatedDateBetween(startOfToday, startOfToday.plusDays(1));
        long ordersThisMonth = saleOrderRepository.countByCreatedDateBetween(startOfMonth, startOfNextMonth);
        long ordersLastMonth = saleOrderRepository.countByCreatedDateBetween(startOfLastMonth, endOfLastMonth);

        double ordersChangePercent = ordersLastMonth > 0
                ? ((double) (ordersThisMonth - ordersLastMonth) / ordersLastMonth) * 100
                : ordersThisMonth > 0 ? 100 : 0;

        Double revenueThisMonth = saleOrderRepository.sumTotalAmountByCreatedDateBetween(startOfMonth, startOfNextMonth);

        long totalEmployees = personRepository.count();
        long newEmployees = personRepository.countByCreatedDateAfter(startOfMonth);

        long totalWarehouses = warehouseRepository.count();

        long lowStockProducts = productRepository.countLowStockProducts();
        long totalProductsInStock = stockBalanceRepository.countDistinctProductId();

        return DashboardSummaryResponse.builder()
                .pendingTasks(0L)
                .todayAttendance(0L)
                .newArticles(0L)
                .expiringContracts(java.util.Collections.emptyList())
                .ordersToday(ordersToday)
                .ordersThisMonth(ordersThisMonth)
                .ordersChangePercent(Math.round(ordersChangePercent * 100.0) / 100.0)
                .revenueThisMonth(revenueThisMonth != null ? revenueThisMonth : 0)
                .totalEmployees(totalEmployees)
                .newEmployees(newEmployees)
                .totalWarehouses(totalWarehouses)
                .lowStockProducts(lowStockProducts)
                .totalProductsInStock(totalProductsInStock)
                .build();
    }
}
