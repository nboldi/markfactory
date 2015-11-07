import hu.elte.markfactory.annotations.ExamExercise;
import hu.elte.markfactory.annotations.ExamTest;

@ExamTest
public class Tester extends hu.elte.markfactory.testbase.ReflectionTester {
	
	@ExamExercise
	public static void test() {
		
		try {
			staticFieldSet("Tested", "numeric", ((java.lang.Integer) staticFieldValue("Tested", "numeric")) + 10);
			staticFieldSet("Tested", "numeric", ((java.lang.Integer) staticFieldValue("Tested", "numeric")) + 1);
		} catch (hu.elte.markfactory.testbase.MissingProgramElementException e) {
			output(e);
			return;
		} catch (java.lang.Throwable e) {
			e.printStackTrace();
			return;
		}
	}
	
}
