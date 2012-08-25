package com.vaguehope.lookfar.auth;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public final class PasswdGen {

	private static final int INIT_SIZE_BYTES = 512;
	private static final long RESEED_INTERVAL_MILLIES = TimeUnit.HOURS.toMillis(1);
	private static final int SEED_SIZE_BYTES = 16; // FIXME is this a good value for this?
	private static final int PASSWD_SIZE_BITS = 130;
	private static final int PASSWD_RADIX = 32;

	private static final Logger LOG = LoggerFactory.getLogger(PasswdGen.class);
	private static final Supplier<PasswdGen> INSTANCE = Suppliers.memoize(new Factory());

	private static final class Factory implements Supplier<PasswdGen> {

		public Factory () {}

		@Override
		public PasswdGen get () {
			return new PasswdGen();
		}

	}

	public static String makePasswd () {
		return INSTANCE.get().generatePasswd();
	}

	private final SecureRandom sr;
	private final AtomicLong lastSeed = new AtomicLong();

	protected PasswdGen () {
		this.sr = getSr();
		this.sr.nextBytes(new byte[INIT_SIZE_BYTES]);
		LOG.info("SecureRandom provider: {}", this.sr.getProvider());
	}

	private static SecureRandom getSr (){
		try {
			return SecureRandom.getInstance("SHA1PRNG");
		}
		catch (NoSuchAlgorithmException e) {
			return new SecureRandom();
		}
	}

	private String generatePasswd () {
		if (System.currentTimeMillis() - this.lastSeed.get() > RESEED_INTERVAL_MILLIES) {
			this.sr.setSeed(SecureRandom.getSeed(SEED_SIZE_BYTES));
			this.lastSeed.set(System.currentTimeMillis());
		}
		return String.format("%-26s", new BigInteger(PASSWD_SIZE_BITS, this.sr).toString(PASSWD_RADIX)).replace(' ', '_');
	}

}
