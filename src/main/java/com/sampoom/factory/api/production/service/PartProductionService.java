package com.sampoom.factory.api.production.service;

import com.sampoom.factory.api.bom.entity.Bom;
import com.sampoom.factory.api.bom.entity.BomMaterial;
import com.sampoom.factory.api.bom.repository.BomRepository;
import com.sampoom.factory.api.material.entity.FactoryMaterial;
import com.sampoom.factory.api.material.repository.FactoryMaterialRepository;
import com.sampoom.factory.api.part.entity.Part;
import com.sampoom.factory.api.part.repository.PartRepository;
import com.sampoom.factory.common.exception.NotFoundException;
import com.sampoom.factory.common.response.ErrorStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

