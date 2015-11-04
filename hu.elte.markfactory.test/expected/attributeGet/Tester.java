import hu.elte.markfactory.annotations.ExamExercise;
import hu.elte.markfactory.annotations.ExamTest;

@ExamTest
public class Tester extends hu.elte.markfactory.testbase.ReflectionTester {

	@ExamExercise
	public static void test(Object tested) {
		
		try {
			int b = ((java.lang.Integer) fieldValue(tested, "a"));
		} catch (hu.elte.markfactory.testbase.MissingProgramElementException e) {
			output(e);
			return;
		} catch (java.lang.Throwable e) {
			e.printStackTrace();
			return;
		}
	}
	
}
