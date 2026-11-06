package com.esmail.calculator.modes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MatrixVectorEvaluatorTest {

    @BeforeEach
    void setUp() {
        // Reset matrices
        // MatA = Identity 3x3
        MatrixEngine.setMatrix("MATA", new MatrixEngine.Matrix(3, 3, new double[]{
                1, 0, 0,
                0, 1, 0,
                0, 0, 1
        }));
        // MatB = Diagonal [2, 3, 4]
        MatrixEngine.setMatrix("MATB", new MatrixEngine.Matrix(3, 3, new double[]{
                2, 0, 0,
                0, 3, 0,
                0, 0, 4
        }));
        // MatC = 2x2
        MatrixEngine.setMatrix("MATC", new MatrixEngine.Matrix(2, 2, new double[]{
                1, 2,
                3, 4
        }));
        MatrixEngine.setMatrix("MATANS", new MatrixEngine.Matrix(3, 3, new double[]{
                9, 9, 9,
                9, 9, 9,
                9, 9, 9
        }));

        // Reset vectors
        VectorEngine.setVector("VCTA", new VectorEngine.Vector3D(1, 2, 3));
        VectorEngine.setVector("VCTB", new VectorEngine.Vector3D(4, 5, 6));
        VectorEngine.setVector("VCTC", new VectorEngine.Vector3D(1, 0, 0));
        VectorEngine.setVector("VCTANS", new VectorEngine.Vector3D(9, 9, 9));
    }

    @Test
    void testMatrixPrecedenceAndParentheses() {
        // MatA + MatB * MatA: MatB * MatA evaluates first (MatB), then MatA + MatB -> diagonal [3, 4, 5]
        String res = MatrixVectorEvaluator.evaluateMatrix("MatA + MatB * MatA");
        assertEquals("[3, 0, 0; 0, 4, 0; 0, 0, 5]", res);

        // (MatA + MatB) * MatA -> [3, 0, 0; 0, 4, 0; 0, 0, 5]
        String resParen = MatrixVectorEvaluator.evaluateMatrix("(MatA + MatB) * MatA");
        assertEquals("[3, 0, 0; 0, 4, 0; 0, 0, 5]", resParen);
    }

    @Test
    void testMatrixScalarMultiplication() {
        // 2MatA, 2 * MatA, MatA * 2
        assertEquals("[2, 0, 0; 0, 2, 0; 0, 0, 2]", MatrixVectorEvaluator.evaluateMatrix("2MatA"));
        assertEquals("[2, 0, 0; 0, 2, 0; 0, 0, 2]", MatrixVectorEvaluator.evaluateMatrix("2 * MatA"));
        assertEquals("[2, 0, 0; 0, 2, 0; 0, 0, 2]", MatrixVectorEvaluator.evaluateMatrix("MatA * 2"));
    }

    @Test
    void testMatrixPowersAndInverse() {
        // MatB^2 -> diagonal [4, 9, 16]
        assertEquals("[4, 0, 0; 0, 9, 0; 0, 0, 16]", MatrixVectorEvaluator.evaluateMatrix("MatB^2"));
        assertEquals("[4, 0, 0; 0, 9, 0; 0, 0, 16]", MatrixVectorEvaluator.evaluateMatrix("MatB²"));

        // MatC^-1 -> 2x2 inverse
        // det(MatC) = 1*4 - 2*3 = -2. inv = [-2, 1; 3/2, -1/2]
        String inv = MatrixVectorEvaluator.evaluateMatrix("MatC^-1");
        assertTrue(inv.contains("-2") && (inv.contains("3/2") || inv.contains("1.5")));
    }

    @Test
    void testMatrixDeterminantAndTranspose() {
        assertEquals("1", MatrixVectorEvaluator.evaluateMatrix("det(MatA)"));
        assertEquals("1", MatrixVectorEvaluator.evaluateMatrix("Abs(MatA)"));
        assertEquals("24", MatrixVectorEvaluator.evaluateMatrix("det(MatB)"));

        // Trn
        String trn = MatrixVectorEvaluator.evaluateMatrix("Trn(MatC)");
        assertEquals("[1, 3; 2, 4]", trn);
    }

    @Test
    void testMatrixAnsPreservationOnFailure() {
        MatrixEngine.Matrix originalAns = MatrixEngine.getMatrix("MATANS");
        assertNotNull(originalAns);

        // Incompatible addition (3x3 + 2x2) must fail with Dimension ERROR
        assertThrows(RuntimeException.class, () -> MatrixVectorEvaluator.evaluateMatrix("MatA + MatC"));

        // MATANS must remain unchanged!
        assertEquals(originalAns, MatrixEngine.getMatrix("MATANS"));
    }

    @Test
    void testVectorDotCrossAndScalar() {
        // VctA dot VctB = 1*4 + 2*5 + 3*6 = 32
        assertEquals("32", MatrixVectorEvaluator.evaluateVector("VctA dot VctB"));

        // VctA cross VctB = [-3, 6, -3]
        assertEquals("[-3, 6, -3]", MatrixVectorEvaluator.evaluateVector("VctA cross VctB"));

        // Scalar: 2VctA
        assertEquals("[2, 4, 6]", MatrixVectorEvaluator.evaluateVector("2VctA"));
        assertEquals("[2, 4, 6]", MatrixVectorEvaluator.evaluateVector("2 * VctA"));
    }

    @Test
    void testVectorParenthesesAndCompoundOperations() {
        // (VctA + VctB) cross VctC
        // VctA + VctB = [5, 7, 9]. VctC = [1, 0, 0].
        // Cross product: [7*0 - 9*0, 9*1 - 5*0, 5*0 - 7*1] = [0, 9, -7]
        assertEquals("[0, 9, -7]", MatrixVectorEvaluator.evaluateVector("(VctA + VctB) cross VctC"));

        // Scalar triple product: VctA dot (VctB cross VctC)
        // VctB cross VctC = [4,5,6] x [1,0,0] = [0, 6, -5]
        // VctA dot [0, 6, -5] = 1*0 + 2*6 + 3*(-5) = 12 - 15 = -3
        assertEquals("-3", MatrixVectorEvaluator.evaluateVector("VctA dot (VctB cross VctC)"));
    }

    @Test
    void testVectorNormAndAngle() {
        // Abs(VctC) = 1
        assertEquals("1", MatrixVectorEvaluator.evaluateVector("Abs(VctC)"));

        // Abs([3, 4, 0])
        VectorEngine.setVector("VCTA", new VectorEngine.Vector3D(3, 4, 0));
        assertEquals("5", MatrixVectorEvaluator.evaluateVector("Abs(VctA)"));

        // Angle between [1, 0, 0] and [0, 1, 0] = 90 degrees
        VectorEngine.setVector("VCTA", new VectorEngine.Vector3D(1, 0, 0));
        VectorEngine.setVector("VCTB", new VectorEngine.Vector3D(0, 1, 0));
        assertEquals("90", MatrixVectorEvaluator.evaluateVector("Angle(VctA, VctB)", true));
    }

    @Test
    void testVectorAnsPreservationOnFailure() {
        VectorEngine.Vector3D originalAns = VectorEngine.getVector("VCTANS");
        assertNotNull(originalAns);

        // Invalid syntax must fail
        assertThrows(RuntimeException.class, () -> MatrixVectorEvaluator.evaluateVector("VctA + (VctB"));

        // VCTANS must remain unchanged!
        assertEquals(originalAns, VectorEngine.getVector("VCTANS"));
    }
}
