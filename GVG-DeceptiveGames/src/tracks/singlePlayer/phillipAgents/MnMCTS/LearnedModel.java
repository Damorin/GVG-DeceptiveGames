package tracks.singlePlayer.phillipAgents.MnMCTS;

public class LearnedModel {
    private double[] w;
    private double epsilon;
    private double C;

    public LearnedModel(int nFeat, double epsilon, double C, double w0) {
        this.w = new double[nFeat];
        for (int i=0; i<w.length; i++)
            w[i] = w0;
        this.epsilon = epsilon;
        this.C = C;
    }

    private double dot(double[] v1, double[] v2) {
        double res = 0.;
        int len = v1.length < v2.length ? v1.length : v2.length;
        for (int i=0; i<len; i++)
            res += v1[i] * v2[i];
        return res;
    }

    private double epsInsLoss(double yp, double y, double epsilon) {
        double z = Math.abs(yp - y) - epsilon;
        return z > 0 ? z : 0;
    }

    public double predict(double[] x) {
        return dot(w, x);
    }

    public void learn(double[] x, double y) {
        double yp = predict(x);
        double loss = epsInsLoss(yp, y, epsilon);
        double tau = loss / dot(x, x);
        tau = tau < C ? tau : C;
        tau = Math.signum(y - yp) * tau;
        for (int i=0; i<w.length; i++)
            w[i] += x[i] * tau;
//System.out.println(x[0] + " " + x[1] + " " + x[2] + " " + x[3] + " " + x[4] + " " + x[5] + " " + x[6] + " : " + y);
//System.out.println(w[0] + " " + w[1] + " " + w[2] + " " + w[3] + " " + w[4] + " " + w[5] + " " + w[6]);
    }
}
