package com.ensap.siteprofile.service;

import com.ensap.siteprofile.dto.DeviceResponse;
import com.ensap.siteprofile.dto.NetworkProfileResponse;
import com.ensap.siteprofile.dto.SiteDetailResponse;
import com.ensap.siteprofile.dto.SiteResponse;
import com.ensap.siteprofile.dto.SubnetResponse;
import com.ensap.siteprofile.dto.VlanResponse;
import com.ensap.siteprofile.dto.WanCircuitResponse;
import com.ensap.siteprofile.entity.Device;
import com.ensap.siteprofile.entity.NetworkProfile;
import com.ensap.siteprofile.entity.Site;
import com.ensap.siteprofile.entity.Subnet;
import com.ensap.siteprofile.entity.Vlan;
import com.ensap.siteprofile.entity.WanCircuit;

import java.util.List;

/** Entity → wire-DTO conversions, kept in one place to avoid repeating field lists per handler. */
final class SiteMapper {

    private SiteMapper() {
    }

    static SiteResponse toResponse(Site site) {
        return new SiteResponse(site.getId(), site.getName(), site.getRegion(), site.getStatus(), site.getLastRefreshedAt());
    }

    static SiteDetailResponse toDetail(Site site, List<Device> devices, List<NetworkProfile> profiles,
                                        List<Vlan> vlans, List<Subnet> subnets, List<WanCircuit> circuits) {
        List<DeviceResponse> deviceResponses = devices.stream().map(SiteMapper::toResponse).toList();
        List<NetworkProfileResponse> profileResponses = profiles.stream()
                .map(p -> toResponse(p,
                        vlans.stream().filter(v -> v.getNetworkProfileId().equals(p.getId())).toList(),
                        subnets.stream().filter(s -> s.getNetworkProfileId().equals(p.getId())).toList()))
                .toList();
        List<WanCircuitResponse> circuitResponses = circuits.stream().map(SiteMapper::toResponse).toList();
        return new SiteDetailResponse(site.getId(), site.getName(), site.getRegion(), site.getStatus(),
                site.getSourceSystem(), site.getLastRefreshedAt(), deviceResponses, profileResponses, circuitResponses);
    }

    static DeviceResponse toResponse(Device device) {
        return new DeviceResponse(device.getId(), device.getSiteId(), device.getType(), device.getVendor(),
                device.getModel(), device.getSerialNumber(), device.getStatus(), device.getCreatedAt());
    }

    static NetworkProfileResponse toResponse(NetworkProfile profile, List<Vlan> vlans, List<Subnet> subnets) {
        return new NetworkProfileResponse(profile.getId(), profile.getSiteId(), profile.getProfileName(),
                profile.getCreatedAt(), vlans.stream().map(SiteMapper::toResponse).toList(),
                subnets.stream().map(SiteMapper::toResponse).toList());
    }

    static VlanResponse toResponse(Vlan vlan) {
        return new VlanResponse(vlan.getId(), vlan.getVlanTag(), vlan.getName());
    }

    static SubnetResponse toResponse(Subnet subnet) {
        return new SubnetResponse(subnet.getId(), subnet.getCidr(), subnet.getPurpose());
    }

    static WanCircuitResponse toResponse(WanCircuit circuit) {
        return new WanCircuitResponse(circuit.getId(), circuit.getCarrier(), circuit.getCircuitId(), circuit.getBandwidthMbps());
    }
}
