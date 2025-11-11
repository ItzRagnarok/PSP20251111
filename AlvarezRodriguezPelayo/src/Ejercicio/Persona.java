package Ejercicio;

public class Persona extends Thread {
	public Persona(int id) {
		super("P" + id%3);
		
	}

	@Override 
	public void run() { 
		try {
			System.out.println(getName() + " esta en el piso ");
			if (Ascensor.PISO.availablePermits() == 0) 
				System.out.println("Ascensor lleno, " + getName() + " espera al siguiente");
			Ascensor.PISO.acquire();
			System.out.println(getName() + " ha entrado al ascensor");
			Thread.sleep((long) (Math.random() * 500));
			Ascensor.PISO.release();
			System.out.println(getName() + "Se ha bajad del ascensor");
		} catch (InterruptedException e){
			
		}
	}
}
