package com.heater.robot;

import com.heater.carbon.AlgaeGrowthModel;
import com.heater.model.LoadTarget;

public final class LoadSelector {

    private LoadSelector() {}

    public static LoadTarget selectDesiredLoad(RouterContext ctx, RouterConfig cfg) {
        if (!ctx.primarySafe()) {
            return LoadTarget.NONE;
        }

        for (LoadTarget target : cfg.priority) {
            if (wantsLoad(target, ctx, cfg)) {
                return target;
            }
        }
        return LoadTarget.NONE;
    }

    private static boolean wantsLoad(LoadTarget target, RouterContext ctx, RouterConfig cfg) {
        RouterThresholds th = cfg.thresholds;
        return switch (target) {
            case HOUSE -> ctx.ambientTemp() < cfg.houseOutsideThreshold
                    && ctx.houseTemp() < ctx.houseSetpoint() - th.houseTempDelta();
            case CARBON_CAPTURE -> ctx.ccsCanRun();
            case ALGAE -> AlgaeGrowthModel.lightFactor(ctx.simTime()) > 0.01
                    && (ctx.algaeTemp() < ctx.algaeOptimalTemp() - th.algaeTempDelta()
                    || ctx.algaeTemp() <= ctx.algaeOptimalTemp() + th.algaeTempDelta());
            case POOL -> ctx.poolTemp() < ctx.poolSetpoint() - th.poolTempDelta();
            case AQUACULTURE -> ctx.aquacultureTemp() < ctx.aquacultureSetpoint() - th.poolTempDelta();
            case PLASTIC_RECYCLING -> ctx.plasticDirectTemp() < ctx.plasticDirectSetpoint() - th.poolTempDelta()
                    || ctx.plasticBoostCanRun();
            case BUFFER -> ctx.bufferTemp() < th.bufferChargeBelow();
            default -> false;
        };
    }
}
