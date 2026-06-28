package com.caimanproject.mapper;

import com.caimanproject.contracts.util.Constants;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.time.ZoneId;

@Mapper(componentModel = Constants.MAPSTRUCT_COMPONENT_MODEL, unmappedTargetPolicy = ReportingPolicy.ERROR)
public class ZoneIdMapper {

    public String fromZoneId(final ZoneId zoneId) {
        if (zoneId == null) {
            return null;
        }

        return zoneId.getId();
    }

    public ZoneId fromString(final String zoneId) {
        if (zoneId == null || zoneId.isBlank()) {
            return null;
        }

        return ZoneId.of(zoneId);
    }
}
