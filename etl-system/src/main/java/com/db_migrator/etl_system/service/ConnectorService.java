package com.db_migrator.etl_system.service;

import com.db_migrator.etl_system.dto.request.ConnectorCreateRequest;
import com.db_migrator.etl_system.dto.request.ConnectorUpdateRequest;
import com.db_migrator.etl_system.dto.response.ConnectorResponse;
import com.db_migrator.etl_system.dto.response.ConnectionTestResponse;
import com.db_migrator.etl_system.mapper.ResponseMapper;
import com.db_migrator.etl_system.model.entity.connector.Connector;
import com.db_migrator.etl_system.model.entity.user.User;
import com.db_migrator.etl_system.repository.ConnectorRepository;
import com.db_migrator.etl_system.repository.SystemScanRepository;
import com.db_migrator.etl_system.security.SecurityUtils;
import com.db_migrator.etl_system.service.metadata.MetadataExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConnectorService {

    private final ConnectorRepository connectorRepository;
    private final SystemScanRepository systemScanRepository;
    private final SecurityUtils securityUtils;
    private final ResponseMapper responseMapper;
    private final MetadataExtractionService metadataExtractionService;

    @Transactional
    public ConnectorResponse create(ConnectorCreateRequest request) {
        Long orgId = securityUtils.getCurrentOrganizationId();
        User currentUser = securityUtils.getCurrentUser();

        if (connectorRepository.existsByNameAndCreatedBy_Organization_Id(request.getName(), orgId)) {
            throw new RuntimeException("Connector with name '" + request.getName() + "' already exists in your organization");
        }

        Connector connector = buildConnector(request, currentUser);
        connector = connectorRepository.save(connector);

        return responseMapper.toConnectorResponse(connector);
    }

    public List<ConnectorResponse> findAll() {
        Long orgId = securityUtils.getCurrentOrganizationId();
        List<Connector> connectors = connectorRepository.findByCreatedBy_Organization_Id(orgId);
        return connectors.stream()
                .map(responseMapper::toConnectorResponse)
                .collect(Collectors.toList());
    }

    public ConnectorResponse findById(Long id) {
        Long orgId = securityUtils.getCurrentOrganizationId();
        Connector connector = connectorRepository
                .findByIdAndCreatedBy_Organization_Id(id, orgId)
                .orElseThrow(() -> new RuntimeException("Connector not found"));
        return responseMapper.toConnectorResponse(connector);
    }

    @Transactional
    public ConnectorResponse update(Long id, ConnectorUpdateRequest request) {
        Long orgId = securityUtils.getCurrentOrganizationId();
        Connector connector = connectorRepository
                .findByIdAndCreatedBy_Organization_Id(id, orgId)
                .orElseThrow(() -> new RuntimeException("Connector not found"));

        if (request.getName() != null) {
            if (!request.getName().equals(connector.getName()) &&
                    connectorRepository.existsByNameAndCreatedBy_Organization_Id(request.getName(), orgId)) {
                throw new RuntimeException("Connector with name '" + request.getName() + "' already exists");
            }
            connector.setName(request.getName());
        }
        if (request.getDatabaseType() != null) {
            connector.setDatabaseType(request.getDatabaseType());
        }
        if (request.getConnectorType() != null) {
            connector.setConnectorType(request.getConnectorType());
        }
        if (request.getHost() != null) {
            connector.setHost(request.getHost());
        }
        if (request.getPort() != null) {
            connector.setPort(request.getPort());
        }
        if (request.getDatabaseName() != null) {
            connector.setDatabaseName(request.getDatabaseName());
        }
        if (request.getUsername() != null) {
            connector.setUsername(request.getUsername());
        }
        if (request.getPassword() != null) {
            connector.setPassword(request.getPassword());
        }

        connector.setUpdatedAt(new Date());
        connector = connectorRepository.save(connector);
        return responseMapper.toConnectorResponse(connector);
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = securityUtils.getCurrentOrganizationId();
        Connector connector = connectorRepository
                .findByIdAndCreatedBy_Organization_Id(id, orgId)
                .orElseThrow(() -> new RuntimeException("Connector not found"));

        // Check if connector has any system scans
        if (systemScanRepository.existsBySourceConnector_Id(connector.getId())) {
            throw new RuntimeException("Cannot delete connector '" + connector.getName() +
                    "' because it has associated system scans. Please delete the scans first.");
        }

        connectorRepository.delete(connector);
    }

    public ConnectionTestResponse testConnection(Long id) {
        Long orgId = securityUtils.getCurrentOrganizationId();
        Connector connector = connectorRepository
                .findByIdAndCreatedBy_Organization_Id(id, orgId)
                .orElseThrow(() -> new RuntimeException("Connector not found"));

        Map<String, String> result = metadataExtractionService.testConnection(connector);
        boolean success = Boolean.parseBoolean(result.get("success"));
        String message = result.get("message");
        String databaseVersion = result.get("databaseVersion");

        return responseMapper.toConnectionTestResponse(success, message, databaseVersion);
    }

    private Connector buildConnector(ConnectorCreateRequest request, User user) {
        Connector connector = new Connector();
        connector.setName(request.getName());
        connector.setDatabaseType(request.getDatabaseType());
        connector.setConnectorType(request.getConnectorType());
        connector.setHost(request.getHost());
        connector.setPort(request.getPort());
        connector.setDatabaseName(request.getDatabaseName());
        connector.setUsername(request.getUsername());
        connector.setPassword(request.getPassword());
        connector.setCreatedBy(user);
        connector.setCreatedAt(new Date());
        connector.setUpdatedAt(new Date());
        return connector;
    }
}
