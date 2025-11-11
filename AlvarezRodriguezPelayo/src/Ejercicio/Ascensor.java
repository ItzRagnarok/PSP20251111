package Ejercicio;

import java.util.concurrent.Semaphore;


public class Ascensor {
	private static int NUMPERSONAS = 100;
	private static int capacidad = 8;
	private static int pisoActual;
	public final static Semaphore PISO = new Semaphore(capacidad);
	
	public static void main(String[] args) throws InterruptedException {
		Thread[] personas = new Thread[NUMPERSONAS]; 
		for (int i=0; i<NUMPERSONAS; i++) 
			(personas[i] = new Persona(i + 1)).start(); 
			for (int i=0; i<NUMPERSONAS; i++) 
			personas[i].join(); 
	}
	
}
