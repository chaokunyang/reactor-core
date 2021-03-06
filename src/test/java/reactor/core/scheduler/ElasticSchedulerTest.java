/*
 * Copyright (c) 2011-2017 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.core.scheduler;

import org.junit.Test;
import reactor.core.Disposable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Maldini
 */
public class ElasticSchedulerTest extends AbstractSchedulerTest {

	@Override
	protected Scheduler scheduler() {
		return Schedulers.elastic();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unsupportedStart() {
		Schedulers.elastic().start();
	}

	@Test(expected = IllegalArgumentException.class)
	public void negativeTime() throws Exception {
		Schedulers.newElastic("test", -1);
	}

	@Override
	protected boolean shouldCheckInterrupted() {
		return true;
	}

	@Test(timeout = 10000)
	public void eviction() throws Exception {
		Scheduler s = Schedulers.newElastic("test-recycle", 2);
		((ElasticScheduler)s).evictor.shutdownNow();

		try{
			Disposable d = (Disposable)s.schedule(() -> {
				try {
					Thread.sleep(10000);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});

			d.dispose();

			while(((ElasticScheduler)s).cache.peek() != null){
				((ElasticScheduler)s).eviction();
				Thread.sleep(100);
			}
		}
		finally {
			s.shutdown();
			s.dispose();//noop
		}

		assertThat(((ElasticScheduler)s).cache).isEmpty();
		assertThat(s.isDisposed()).isTrue();
	}
}
