package com.cms.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * APS network keys. Values come from the gitignored application-local.properties
 * or from the environment. They are never hardcoded.
 *
 * awk.acq and awk.isr are both PIN-block keys (PVK): acquirer side and issuer side.
 * component1..3 are the three parts of the ZMK, combined by XOR.
 */
@Data
@ConfigurationProperties(prefix = "network.key")
public class NetworkKeyProperties {

    private Awk awk = new Awk();
    private HexValue component1 = new HexValue();
    private HexValue component2 = new HexValue();
    private HexValue component3 = new HexValue();
    /** Which PVK opens an incoming PIN block: issuer (default) or acquirer. */
    private Pinblock pinblock = new Pinblock();

    @Data
    public static class Awk {
        private HexValue acq = new HexValue();
        private HexValue isr = new HexValue();
    }

    @Data
    public static class HexValue {
        private String hex;
    }

    @Data
    public static class Pinblock {
        private String side = "issuer";
    }
}
