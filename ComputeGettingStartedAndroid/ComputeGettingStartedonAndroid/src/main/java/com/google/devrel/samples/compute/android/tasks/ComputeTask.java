package com.google.devrel.samples.compute.android.tasks;

import android.os.AsyncTask;

/**
 * Base class for {@code AsyncTask}s that work with the Google Compute Engine API.
 *
 * @author paulrashidi@google.com (Paul Rashidi)
 */
public abstract class ComputeTask<Params, Progress, Result> extends
    AsyncTask<Params, Progress, Result>  {
}
