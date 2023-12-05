
public class Plane extends Vehicle {

    public void board(Passenger p){
        System.out.println("Passenger going on board plane");
        p.fastenSeatBeltIn(this);
    }

}
