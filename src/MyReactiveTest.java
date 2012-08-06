import static org.junit.Assert.*;
import hu.akarnokd.reactive4java.base.Action1;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Option;
import hu.akarnokd.reactive4java.query.IterableBuilder;
import hu.akarnokd.reactive4java.query.ObservableBuilder;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Observer;
import hu.akarnokd.reactive4java.reactive.Reactive;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;


public class MyReactiveTest {
	@Test
	public void testChain() throws InterruptedException {
		final List<Integer> list = Arrays.asList(0, 1, 2);
		final List<Integer> words = Arrays.asList(3, 4, 5);

		Reactive.run(
		ObservableBuilder.from(list)
		.selectMany(new Func1<Integer, Observable<? extends Integer>>() {
			@Override
			public Observable<? extends Integer> invoke(final Integer param1) {
				return Reactive.createWithCloseable(new Func1<Observer<? super Integer>, Closeable>() {
					@Override
					public Closeable invoke(Observer<? super Integer> observer) {
						try {
							// 時間がかかる処理を行っても正しい順番が維持されるかを確認するために、少し待つ
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							fail(e.getMessage());
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
		.invoke(Reactive.println())
		.invoke(new Action1<Integer>() {
			@Override
			public void invoke(Integer param1) {
				System.out.println("invoke1c called:" + words.get(param1));
			}
		})
		, Reactive.asObserver(new Action1<Option<Integer>>() {
			@Override
			public void invoke(Option<Integer> value) {
			}
		}));
		
		
	}

	/**
	 * データソースに 0〜のインデックスを付けて後続に流せるのかのテスト(その1)
	 * 
	 * buffer で List 化して、index を付けて from で Observable 化する。まわりくどいし buffer が微妙。
	 * @throws InterruptedException 
	 */
	@Test
	public void testAppendIndexToArray1() throws InterruptedException {
		
		List<String> list = Arrays.asList("aaa", "bbb", "ccc");
		final List<SimpleEntry<Integer, String>> actualList = new ArrayList<SimpleEntry<Integer,String>>();
		
		Reactive.run(
		ObservableBuilder.from(list)
		.buffer(3) // TODO 微妙…
		.selectMany(new Func1<List<String>, Observable<? extends SimpleEntry<Integer, String>>>() {
			@Override
			public Observable<? extends SimpleEntry<Integer, String>> invoke(
					List<String> list) {
				List<SimpleEntry<Integer, String>> indexWithStringList = new ArrayList<SimpleEntry<Integer,String>>();
				for (int i = 0; i < list.size(); i++) {
					indexWithStringList.add(new SimpleEntry<Integer, String>(i, list.get(i)));
				}
				return ObservableBuilder.from(indexWithStringList);
			}
		})
		.invoke(new Action1<SimpleEntry<Integer, String>>() {
			@Override
			public void invoke(SimpleEntry<Integer, String> value) {
				actualList.add(value);
			}
		})
		, Reactive.println()); // run はメインスレッドで実行されるからここで終わるの待ってる
		
		assertEquals(Integer.valueOf(0), actualList.get(0).getKey());
		assertEquals(Integer.valueOf(1), actualList.get(1).getKey());
		assertEquals(Integer.valueOf(2), actualList.get(2).getKey());
		assertEquals("aaa", actualList.get(0).getValue());
		assertEquals("bbb", actualList.get(1).getValue());
		assertEquals("ccc", actualList.get(2).getValue());
	}

	/**
	 * データソースに 0〜のインデックスを付けて後続に流せるのかのテスト(その2)
	 * 
	 * selectMany の selector にインスタンス変数でカウンタを持たせる方法。まあよさげ。
	 * @throws InterruptedException 
	 */
	@Test
	public void testAppendIndexToArray2() throws InterruptedException {
		
		List<String> list = Arrays.asList("aaa", "bbb", "ccc");
		final List<SimpleEntry<Integer, String>> actualList = new ArrayList<SimpleEntry<Integer,String>>();
		
		Reactive.run(
		ObservableBuilder.from(list)
		.selectMany(new Func1<String, Observable<? extends SimpleEntry<Integer, String>>>() {
			private int i; 
			
			@Override
			public Observable<? extends SimpleEntry<Integer, String>> invoke(String param1) {
				Observable<SimpleEntry<Integer, String>> observable = Reactive.singleton(new SimpleEntry<Integer, String>(i, param1));
				i++;
				return observable;
			}
		})
		.invoke(new Action1<SimpleEntry<Integer, String>>() {
			@Override
			public void invoke(SimpleEntry<Integer, String> value) {
				actualList.add(value);
			}
		})
		, Reactive.println()); // run はメインスレッドで実行されるからここで終わるの待ってる
		
		assertEquals(Integer.valueOf(0), actualList.get(0).getKey());
		assertEquals(Integer.valueOf(1), actualList.get(1).getKey());
		assertEquals(Integer.valueOf(2), actualList.get(2).getKey());
		assertEquals("aaa", actualList.get(0).getValue());
		assertEquals("bbb", actualList.get(1).getValue());
		assertEquals("ccc", actualList.get(2).getValue());
	}

	@Test
	public void testWindow() throws InterruptedException {
		Reactive.run(
		ObservableBuilder.tick(1, TimeUnit.SECONDS)
		.window(5, TimeUnit.SECONDS)
		.take(1)
		.first()
		, Reactive.println());
	}
}
