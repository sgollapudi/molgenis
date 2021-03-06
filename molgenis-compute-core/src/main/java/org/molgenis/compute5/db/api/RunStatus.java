package org.molgenis.compute5.db.api;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class RunStatus
{
	private final int generated;
	private final int ready;
	private final int running;
	private final int failed;
	private final int done;

	public RunStatus(int generated, int ready, int running, int failed, int done)
	{
		this.generated = generated;
		this.ready = ready;
		this.running = running;
		this.done = done;
		this.failed = failed;
	}

	public int getGenerated()
	{
		return generated;
	}

	public int getReady()
	{
		return ready;
	}

	public int getRunning()
	{
		return running;
	}

	public int getDone()
	{
		return done;
	}

	public int getFailed()
	{
		return failed;
	}

	@Override
	public String toString()
	{
		return ToStringBuilder.reflectionToString(this);
	}

}
