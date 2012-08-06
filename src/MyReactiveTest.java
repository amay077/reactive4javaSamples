import static org.junit.Assert.*;
import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.query.ObservableBuilder;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Observer;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


public class MyReactiveTest {

	@Test
	public void testChain() {
		final List<Integer> list = new ArrayList<Integer>();
		list.add(0);
		list.add(1);
		list.add(2);
		list.add(3);
		list.add(4);

		final List<String> words = new ArrayList<String>();
		words.add("AAA");
		words.add("BBB");
		words.add("CCC");
		words.add("DDD");
		words.add("EEE");

		try {
			Reactive.run(
			ObservableBuilder.range(0, 5)			
			.selectMany(new Func1<Integer, Observable<? extends Integer>>() {
				@Override
				public Observable<? extends Integer> invoke(final Integer param1) {
					return Reactive.createWithCloseable(new Func1<Observer<? super Integer>, Closeable>() {
						@Override
						public Closeable invoke(Observer<? super Integer> observer) {
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							observer.next(param1);
							observer.finish();
							return new Closeable() {
								@Override
								public void close() throws IOException {
								}
							};
						}
					});
				}
			})
			.invoke(new Action1<Integer>() {
				@Override
				public void invoke(Integer value) {
					System.out.println("invoke1a called:" + words.get(value));
				}
			})
			.invoke(Reactive.println())
			.invoke(this.debugPrint())
			.selectMany(new Func1<Integer, Observable<? extends String>>() {
				@Override
				public Observable<? extends String> invoke(Integer param1) {
					System.out.println("invoke1c called:" + words.get(param1));
					return Reactive.singleton(words.get(param1));
				}
			})
			, Reactive.println());
		} catch (InterruptedException e1) {
			fail(e1.getMessage());
		}
	}

	private <T> Action1<T> debugPrint() {
		return new Action1<T>() {
			@Override
			public void invoke(T param) {
				System.out.println("original debug code:" + param);
			}
		};
	}
}
