package com.example.warehouse_proj.truck;

import com.example.warehouse_proj.common.BaseRepository;

import java.util.UUID;

public interface TruckRepository extends BaseRepository<Truck> {

    boolean existsByPlateNumberIgnoreCaseAndActiveTrue(String plateNumber);

    boolean existsByPlateNumberIgnoreCaseAndActiveTrueAndIdNot(String plateNumber, UUID id);
}
