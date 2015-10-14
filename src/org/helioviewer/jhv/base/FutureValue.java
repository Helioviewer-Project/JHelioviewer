package org.helioviewer.jhv.base;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;

public class FutureValue<T> implements Future<T>
{
	private @Nullable T value;
	
	public FutureValue(@Nullable T _value)
	{
		value = _value;
	}
	
	@Override
	public boolean cancel(boolean _mayInterruptIfRunning)
	{
		return false;
	}

	@Override
	public @Nullable T get() throws InterruptedException, ExecutionException
	{
		return value;
	}

	@Override
	public @Nullable T get(long _timeout, @Nullable TimeUnit _unit) throws InterruptedException, ExecutionException, TimeoutException
	{
		return value;
	}

	@Override
	public boolean isCancelled()
	{
		return false;
	}

	@Override
	public boolean isDone()
	{
		return true;
	}
}
