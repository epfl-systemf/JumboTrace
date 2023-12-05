
public class PrivateJet extends Plane {

    public void board(VipPassenger p){
        System.out.println("VIP going on board private jet");
        p.fastenSeatBeltIn(this);
    }

}
