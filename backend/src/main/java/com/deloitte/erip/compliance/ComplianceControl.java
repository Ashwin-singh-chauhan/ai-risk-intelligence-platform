package com.deloitte.erip.compliance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "compliance_controls")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceControl {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplianceFramework framework;

    @Column(name = "control_id", nullable = false)
    private String controlId;

    @Column(name = "control_name", nullable = false)
    private String controlName;

    @Column(name = "control_description", columnDefinition = "TEXT")
    private String controlDescription;

    private String category;
}
