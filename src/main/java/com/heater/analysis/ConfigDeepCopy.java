package com.heater.analysis;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

@SuppressWarnings("unchecked")
public final class ConfigDeepCopy {

    private ConfigDeepCopy() {}

    public static Map<String, Object> copy(Map<String, Object> source) {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(opts);
        return yaml.load(yaml.dump(source));
    }
}
