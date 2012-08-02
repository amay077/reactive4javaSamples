import static org.junit.Assert.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Date;
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
	public void sample_04_Tick_bacic() {
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
	public void sample_04_Generate_bacic() {
		
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
	
	/***
	 * Sample of ObservableBuilder.buffer()
	 * 
	 * @see http://d.hatena.ne.jp/okazuki/20120117/1326804922
	 */
	@Test
	public void sample_16_Min_Max_Ave() {
		try {
			ObservableBuilder<Integer> s = ObservableBuilder.range(1, 3);

			// 最大値を求めて表示
			Reactive.run(
			    s.<Integer>max()
			, new Observer<Integer>() {
				@Override
				public void next(Integer value) { System.out.println("max:" + value); }

				@Override
				public void error(Throwable ex) { System.out.println("error:" + ex.getMessage()); }

				@Override
				public void finish() { 
					System.out.println("max finish!"); 
					assertTrue(true);
				}
			});
			
			// 最小値を求めて表示
			Reactive.run(
                s.<Integer>min()
            , new Observer<Integer>() {
				@Override
				public void next(Integer value) { System.out.println("min:" + value); }

				@Override
				public void error(Throwable ex) { System.out.println("error:" + ex.getMessage()); }

				@Override
				public void finish() { 
					System.out.println("min finish!"); 
					assertTrue(true);
				}
			});

			// 平均を求めて表示
			Reactive.run(
			    s.averageInt()
			, new Observer<Double>() {
				@Override
				public void next(Double value) { System.out.println("average:" + value); }

				@Override
				public void error(Throwable ex) { System.out.println("error:" + ex.getMessage()); }

				@Override
				public void finish() { 
					System.out.println("average finish!"); 
					assertTrue(true);
				}
			});

		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
	}


	/***
	 * Sample of ObservableBuilder.buffer()
	 * 
	 * @see http://d.hatena.ne.jp/okazuki/20120117/1326804922
	 */
	@Test
	public void sample_25_Buffer_bacic() {
		try {
			// 購読
			Reactive.run(
				// 1～10の値を発行するIObservable<int>のシーケンス
				ObservableBuilder.range(1, 10)
				// 3つずつの値に分ける
				.buffer(3)
			, new Observer<List<Integer>>() {
				@Override
				public void next(List<Integer> l) {
					// List<int>の内容を出力
		            System.out.println("-- Buffer start");
		            for (int i : l)
		            {
			            System.out.println(i);
		            }
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

	/***
	 * Sample of ObservableBuilder.buffer()
	 * 
	 * @see http://d.hatena.ne.jp/okazuki/20120117/1326804922
	 */
	@Test
	public void sample_25_Buffer_time() {
		final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		try {
			// 購読
			Reactive.run(
				// 500msごとに値を発行する
				ObservableBuilder.tick(500, TimeUnit.MILLISECONDS)
				// 3秒間値を溜める
				.buffer(3, TimeUnit.SECONDS)
				// 最初の3つを後続に流す
				.take(3)
			, new Observer<List<Long>>() {
				@Override
				public void next(List<Long> l) {
					// List<int>の内容を出力
		            System.out.println("-- Buffer " + dateFormat.format(new Date()));
		            for (long i : l)
		            {
			            System.out.println(i);
		            }
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

	/***
	 * Sample of ObservableBuilder.buffer()
	 * 
	 * @see http://d.hatena.ne.jp/okazuki/20120117/1326804922
	 */
	@Test
	public void sample_25_Buffer_combi() {
		try {
			// 購読
			Reactive.run(
				// 500msごとに値を発行する
				ObservableBuilder.tick(500, TimeUnit.MILLISECONDS)
				// 3秒間値を溜める
				.buffer(3, TimeUnit.SECONDS)
				// 最初の3つを後続に流す
				.take(3)
				.selectMany(new Func1<List<Long>, Observable<? extends Long>>() {
					@Override
					public Observable<? extends Long> invoke(List<Long> l) {
						return ObservableBuilder.from(l);
					}
				})
				.<Long>max()
			, new Observer<Long>() {
				@Override
				public void next(Long l) { System.out.println("next:" + l); }

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
