package org.helioviewer.jhv.base;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureValue<T> implements Future<T>
{
	private T value;
	
	public FutureValue(T _value)
	{
		value = _value;
	}
	
	@Override
	public boolean cancel(boolean _mayInterruptIfRunning)
	{
		return false;
	}

	@Override
	public T get() throws InterruptedException, ExecutionException
	{
		return value;
	}

	@Override
	public T get(long _timeout, TimeUnit _unit) throws InterruptedException, ExecutionException, TimeoutException
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
