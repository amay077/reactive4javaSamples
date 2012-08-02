import static org.junit.Assert.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.xml.crypto.dsig.keyinfo.KeyValue;

import hu.akarnokd.reactive4java.base.Action0;
import hu.akarnokd.reactive4java.base.Func1;
import hu.akarnokd.reactive4java.base.Func2;
import hu.akarnokd.reactive4java.query.ObservableBuilder;
import hu.akarnokd.reactive4java.reactive.Observable;
import hu.akarnokd.reactive4java.reactive.Observer;
import hu.akarnokd.reactive4java.reactive.Reactive;
import hu.akarnokd.reactive4java.reactive.Timestamped;

import org.junit.Test;


public class ObservableBuilderTest {

	/***
	 * Sample of ObservableBuilder.tick()
	 * 
	 * @see http://d.hatena.ne.jp/okazuki/20111106/1320584830
	 */
	@Test
	public void testTick_bacic() {
		// 3秒後から1秒間隔で5回値を発行するIObservable<long>を作成する
		ObservableBuilder<Long> source = ObservableBuilder.tick(
			1, 5, 3, TimeUnit.SECONDS);
		
		try {
			// 購読
			// 3秒後からOnNext(回数)が表示される
			Reactive.run(
				source
				, new Observer<Long>() {
					@Override
					public void next(Long value) { System.out.println("next:" + value); }
	
					@Override
					public void error(Throwable ex) { System.out.println("error:" + ex.getMessage()); }
	
					@Override
					public void finish() { 
						System.out.println("finish!"); 
						assertTrue(true);
					}
				});
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
	}
	
	/***
	 * Sample of ObservableBuilder.generate()
	 * 
	 * @see http://d.hatena.ne.jp/okazuki/20111106/1320584830
	 */
	@Test
	public void testGenerate_bacic() {
		
		ObservableBuilder<Timestamped<Integer>> source = ObservableBuilder.generateTimed(
				0,  // 0から
				new Func1<Integer, Boolean>() {
					@Override
					public Boolean invoke(Integer i) {
						return i < 10; // i < 10以下の間繰り返す
					}
				}, 
				new Func1<Integer, Integer>() {
					@Override
					public Integer invoke(Integer i) {
						return ++i; // iは1ずつ増える
					}
				}, 
				new Func1<Integer, Integer>() {
					@Override
					public Integer invoke(Integer i) {
						return i * i;
					}
				},
				new Func1<Integer, Long>() {
					@Override
					public Long invoke(Integer i) {
						// 値は(発行する値 * 100)ms間隔で発行する
						return  i * 100L; // TODO reactive4java 側で TimeUnit に対応するらしいので、その時は要修正。
					}
				});

		try {
			// 購読
			Reactive.run(source
				, new Observer<Timestamped<Integer>>() {
					@Override
					public void next(Timestamped<Integer> value) { System.out.println("next:" + value.value()); }
	
					@Override
					public void error(Throwable ex) { System.out.println("error:" + ex.getMessage()); }
	
					@Override
					public void finish() { 
						System.out.println("finish!"); 
						assertTrue(true);
					}
				});
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
	}
	
	/***
	 * Sample of ObservableBuilder.resumeAlways()
	 * 
	 * @see http://d.hatena.ne.jp/okazuki/20120211/1328973285
	 */
	@Test
	public void testResumeAlways_bacic() {
		ArrayList<Observable<String>> resumeObservables = new ArrayList<Observable<String>>();
		resumeObservables.add(Reactive.singleton("OK"));
		
		try {
			// 購読
			Reactive.run(
			ObservableBuilder
			    // 例外を出す
			    .throwException(new Exception())
			    // resumeAlwaysでエラーになったときの代わりを指定しておく
			    .resumeAlways(resumeObservables)
			, new Observer<Object>() {
				@Override
				public void next(Object value) { System.out.println("next:" + value.toString()); }

				@Override
				public void error(Throwable ex) { System.out.println("error:" + ex.getMessage()); }

				@Override
				public void finish() { 
					System.out.println("finish!"); 
					assertTrue(true);
				}
			});
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
	}
	
	/***
	 * Sample of ObservableBuilder.resumeAlways()
	 * 
	 * @see http://d.hatena.ne.jp/okazuki/20120211/1328973285
	 */
	@Test
	public void testResumeAlways_resume() {
		List<Observable<Observable<String>>> empty = new ArrayList<Observable<Observable<String>>>();

		try {
			// 購読
			Reactive.run(
			ObservableBuilder.from("NG", "Error", "Abort", "OK")
			.select(new Func2<Integer, String, SimpleEntry<Integer, String>>() {
				@Override
				public SimpleEntry<Integer, String> invoke(Integer i, String s) {
					return new SimpleEntry<Integer, String>(i, s);
				}
			})
			.select( new Func1<SimpleEntry<Integer, String>, Observable<String>>() {
				@Override
				public Observable<String> invoke(SimpleEntry<Integer, String> s) {
					if (s.getValue().compareTo("OK") != 0) {
						return Reactive.throwException(new Exception(s.getValue()));
					} else {
						return Reactive.singleton(s.getValue());
					}
				}
			})
//			.resumeAlways(empty) // <-- ここ実行すると finish! が呼ばれない
			, new Observer<Observable<String>>() {
				@Override
				public void next(Observable<String> value) { 
					System.out.println("next:" + value);  // ← なぜか NG,Error,Abort でも呼ばれる(Reactive.throwException は呼び出されているのに)
				}

				@Override
				public void error(Throwable ex) { System.out.println("error:" + ex.getMessage()); }

				@Override
				public void finish() { 
					System.out.println("finish!"); 
					assertTrue(true);
				}
			});
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
	}
}
