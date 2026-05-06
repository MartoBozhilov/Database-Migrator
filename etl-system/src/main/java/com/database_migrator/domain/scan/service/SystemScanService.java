package com.database_migrator.domain.scan.service;

import com.database_migrator.domain.scan.dto.SystemScanCreateRequest;
import com.database_migrator.domain.scan.dto.SystemScanDetailsResponse;
import com.database_migrator.domain.scan.dto.SystemScanResponse;
import com.database_migrator.domain.common.mapper.ResponseMapper;
import com.database_migrator.domain.connector.model.Connector;
import com.database_migrator.domain.scan.model.SystemScan;
import com.database_migrator.domain.auth.model.User;
import com.database_migrator.domain.connector.model.ConnectorTypeEnum;
import com.database_migrator.domain.scan.model.ScanStatusEnum;
import com.database_migrator.domain.connector.repository.ConnectorRepository;
import com.database_migrator.domain.scan.repository.SystemScanRepository;
import com.database_migrator.domain.transformation.repository.TransformationModelRepository;
import com.database_migrator.domain.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemScanService {

    private final SystemScanRepository scanRepository;
    private final ConnectorRepository connectorRepository;
    private final TransformationModelRepository transformationModelRepository;
    private final MetadataExtractionService metadataExtractionService;
    private final SecurityUtils securityUtils;
    private final ResponseMapper responseMapper;

    @Transactional
    public SystemScanResponse create(SystemScanCreateRequest request) {
        Long orgId = securityUtils.getCurrentOrganizationId();
        User currentUser = securityUtils.getCurrentUser();

        Connector connector = connectorRepository
                .findByIdAndCreatedBy_Organization_Id(request.getSourceConnectorId(), orgId)
                .orElseThrow(() -> new RuntimeException("Source connector not found"));

        if (connector.getConnectorType() != ConnectorTypeEnum.SOURCE) {
            throw new RuntimeException("System scan can only be performed on SOURCE connectors");
        }

        SystemScan scan = buildSystemScanPending(request, connector, currentUser);
        scan = scanRepository.save(scan);

        return responseMapper.toSystemScanResponse(scan);
    }

    @Transactional
    public SystemScanResponse startScan(Long scanId) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        SystemScan scan = scanRepository
                .findByIdAndCreatedBy_Organization_Id(scanId, orgId)
                .orElseThrow(() -> new RuntimeException("System scan not found"));

        if (scan.getStatus() != ScanStatusEnum.PENDING) {
            throw new RuntimeException("System scan can only be started if status is PENDING");
        }

        metadataExtractionService.extractMetadataAsync(scan.getId());

        return responseMapper.toSystemScanResponse(scan);
    }

    public List<SystemScanResponse> findAll() {
        Long orgId = securityUtils.getCurrentOrganizationId();
        List<SystemScan> scans = scanRepository.findByCreatedBy_Organization_Id(orgId);
        return scans.stream()
                .map(responseMapper::toSystemScanResponse)
                .collect(Collectors.toList());
    }

    public SystemScanResponse findById(Long id) {
        Long orgId = securityUtils.getCurrentOrganizationId();
        SystemScan scan = scanRepository
                .findByIdAndCreatedBy_Organization_Id(id, orgId)
                .orElseThrow(() -> new RuntimeException("System scan not found"));
        return responseMapper.toSystemScanResponse(scan);
    }

    public SystemScanDetailsResponse getDetails(Long id) {
        Long orgId = securityUtils.getCurrentOrganizationId();
        SystemScan scan = scanRepository
                .findByIdAndCreatedBy_Organization_Id(id, orgId)
                .orElseThrow(() -> new RuntimeException("System scan not found"));
        return responseMapper.toSystemScanDetailsResponse(scan);
    }

    public List<SystemScanResponse> findByConnectorId(Long connectorId) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        connectorRepository.findByIdAndCreatedBy_Organization_Id(connectorId, orgId)
                .orElseThrow(() -> new RuntimeException("Connector not found"));

        return scanRepository
                .findBySourceConnector_IdAndCreatedBy_Organization_Id(connectorId, orgId)
                .stream()
                .map(responseMapper::toSystemScanResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = securityUtils.getCurrentOrganizationId();
        SystemScan scan = scanRepository
                .findByIdAndCreatedBy_Organization_Id(id, orgId)
                .orElseThrow(() -> new RuntimeException("System scan not found"));

        if (transformationModelRepository.existsBySystemScan_Id(scan.getId())) {
            throw new RuntimeException("Cannot delete scan '" + scan.getName() +
                    "' because it has associated transformation models. Please delete the transformation models first.");
        }

        scanRepository.delete(scan);
    }

    private SystemScan buildSystemScanPending(SystemScanCreateRequest request, Connector connector, User user) {
        return SystemScan.builder()
                .name(request.getName())
                .createdBy(user)
                .sourceConnector(connector)
                .status(ScanStatusEnum.PENDING)
                .createdAt(new Date())
                .tableMetadataList(new ArrayList<>())
                .relationMetadataList(new ArrayList<>())
                .build();
    }
}
