package com.vaguehope.lookfar.threshold;

public class EqualsStringThreshold implements Threshold {

	private final String expectedValue;

	public EqualsStringThreshold (String expectedValue) {
		this.expectedValue = expectedValue;
	}

	@Override
	public ThresholdStatus isValid (String value) {
		return this.expectedValue.equals(value) ? ThresholdStatus.OK : ThresholdStatus.EXCEEDED;
	}

}
