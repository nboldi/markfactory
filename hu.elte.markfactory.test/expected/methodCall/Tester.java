import hu.elte.markfactory.annotations.ExamExercise;
import hu.elte.markfactory.annotations.ExamTest;

@ExamTest
public class Tester extends hu.elte.markfactory.testbase.ReflectionTester {

	@ExamExercise
	public static void test() {
		try {
			call("f", 
					((Object) construct("Tested", new java.lang.String[]{}, new java.lang.Object[]{})), 
					new java.lang.String[]{}, new java.lang.Object[]{}
			);
		} catch (hu.elte.markfactory.testbase.MissingProgramElementException e) {
			output(e);
			return;
		} catch (java.lang.Throwable e) {
			e.printStackTrace();
			return;
		}
	}
	
}
