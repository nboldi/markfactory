import hu.elte.markfactory.annotations.ExamExercise;
import hu.elte.markfactory.annotations.ExamTest;

@ExamTest
public class Tester extends hu.elte.markfactory.testbase.ReflectionTester {

	@ExamExercise
	public static void test(Object tested) {
		
		try {
			fieldSet(tested, "a", 3);
		} catch (hu.elte.markfactory.testbase.MissingProgramElementException e) {
			output(e);
			return;
		} catch (java.lang.Throwable e) {
			e.printStackTrace();
			return;
		}
	}
	
}
