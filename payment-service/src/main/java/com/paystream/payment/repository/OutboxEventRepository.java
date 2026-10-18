package com.paystream.payment.repository;

import java.util.UUID;

import com.paystream.payment.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
}