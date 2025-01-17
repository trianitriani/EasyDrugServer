package it.unipi.EasyDrugServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EasyDrugServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EasyDrugServerApplication.class, args);

		/*
		Drug drug = new Drug();
		drug.setId(2);
		drug.setName("aspirin");
		drug.setPrice(5);
		Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
		drug.setPrescriptionTimestamp(currentTimestamp);
		drug.setQuantity(1);

		PurchaseCartRepository purchase = new PurchaseCartRepository();
		purchase.saveDrugIntoPurchaseCart("MRCSML",drug);

		PurchaseCart purchCart = new PurchaseCart();
		purchCart = purchase.getPurchaseCart("MRCSML");
		System.out.println(purchCart);

		 */
	}

}
