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
package reactor.test.publisher;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Fuseable;
import reactor.core.publisher.FluxSource;
import reactor.core.publisher.Operators;

/**
 * @author Stephane Maldini
 */
final class FluxFuseableExceptionOnPoll<T> extends FluxSource<T, T>
		implements Fuseable {

	@SuppressWarnings("unchecked")
	static <I> void next(Subscriber<I> s, I item) {
		((FuseableExceptionOnPollSubscriber<I>) s).actual.onNext(item);
	}

	@SuppressWarnings("unchecked")
	static <I> void tryNext(Subscriber<I> s, I item) {
		((ConditionalSubscriber<I>) ((FuseableExceptionOnPollSubscriber<I>) s).actual).tryOnNext(
				item);
	}

	@SuppressWarnings("unchecked")
	static <I> boolean shouldTryNext(Subscriber<I> s) {
		return s instanceof FluxFuseableExceptionOnPoll.FuseableExceptionOnPollSubscriber && ((FuseableExceptionOnPollSubscriber) s).actual instanceof ConditionalSubscriber;
	}

	final RuntimeException exception;

	FluxFuseableExceptionOnPoll(Publisher<? extends T> source,
			RuntimeException exception) {
		super(source);
		this.exception = exception;
	}

	@Override
	public void subscribe(Subscriber<? super T> s) {
		source.subscribe(new FuseableExceptionOnPollSubscriber<>(s, exception));
	}

	static final class FuseableExceptionOnPollSubscriber<T>
			implements QueueSubscription<T>, ConditionalSubscriber<T> {

		final Subscriber<? super T> actual;

		final RuntimeException exception;
		QueueSubscription<T> qs;

		FuseableExceptionOnPollSubscriber(Subscriber<? super T> actual,
				RuntimeException exception) {
			this.actual = actual;
			this.exception = exception;
		}

		@Override
		public boolean tryOnNext(T t) {
			throw exception;
		}

		@Override
		public void onSubscribe(Subscription s) {
			this.qs = Operators.as(s);
			actual.onSubscribe(this);
		}

		@Override
		public void onNext(T t) {
			if (t != null) {
				throw exception;
			}
			actual.onNext(null);
		}

		@Override
		public void onError(Throwable t) {
			actual.onError(t);
		}

		@Override
		public void onComplete() {
			actual.onComplete();
		}

		@Override
		public int requestFusion(int requestedMode) {
			return qs.requestFusion(requestedMode);
		}

		@Override
		public T poll() {
			T t = qs.poll();
			if (t != null) {
				throw exception;
			}
			return null;
		}

		@Override
		public int size() {
			return qs.size();
		}

		@Override
		public boolean isEmpty() {
			return qs.isEmpty();
		}

		@Override
		public void clear() {
			qs.clear();
		}

		@Override
		public void request(long n) {
			qs.request(n);
		}

		@Override
		public void cancel() {
			qs.cancel();
		}

	}
}
