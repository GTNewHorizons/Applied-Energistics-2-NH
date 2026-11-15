package appeng.test.behaviors;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.energy.IEnergySource;

final class TestEnergySource implements IEnergySource {

    private double stored;
    private double extracted;

    TestEnergySource(double stored) {
        this.stored = stored;
    }

    static TestEnergySource infinite() {
        return new TestEnergySource(Double.POSITIVE_INFINITY);
    }

    double getExtracted() {
        return this.extracted;
    }

    @Override
    public double extractAEPower(double amt, Actionable mode, PowerMultiplier usePowerMultiplier) {
        final double available = Math.min(amt, this.stored);
        if (mode == Actionable.MODULATE) {
            this.stored -= available;
            this.extracted += available;
        }
        return available;
    }
}
