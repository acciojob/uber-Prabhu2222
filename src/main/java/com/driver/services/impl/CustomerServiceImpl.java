package com.driver.services.impl;

import com.driver.model.TripBooking;
import com.driver.services.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.driver.model.Customer;
import com.driver.model.Driver;
import com.driver.repository.CustomerRepository;
import com.driver.repository.DriverRepository;
import com.driver.repository.TripBookingRepository;
import com.driver.model.TripStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerServiceImpl implements CustomerService {

	@Autowired
	CustomerRepository customerRepository2;

	@Autowired
	DriverRepository driverRepository2;

	@Autowired
	TripBookingRepository tripBookingRepository2;

	@Override
	public void register(Customer customer) {
		//Save the customer in database
		customerRepository2.save(customer);
	}

	@Override
	public void deleteCustomer(Integer customerId) {
		// Delete customer without using deleteById function
		customerRepository2.deleteById(customerId);

	}

	@Override
	public TripBooking bookTrip(int customerId, String fromLocation, String toLocation, int distanceInKm) throws Exception{
		//Book the driver with lowest driverId who is free (cab available variable is Boolean.TRUE). If no driver is available, throw "No cab available!" exception
		//Avoid using SQL query
		List<Driver> driverList=driverRepository2.findAll();
		Collections.sort(driverList,(d1,d2)->{
			if(d1.getDriverId()>d2.getDriverId()) return 1;
			else if(d1.getDriverId()<d2.getDriverId()) return -1;
			else return 0;
		});
		boolean driverFound=false;
		for(Driver ele:driverList){
			if(ele.getCab().isAvailable()){
				Optional<Customer> optionalCustomer=customerRepository2.findById(customerId);
				Customer customer=optionalCustomer.get();

				TripBooking tripBooking=new TripBooking();
				tripBooking.setFromLocation(fromLocation);
				tripBooking.setToLocation(toLocation);
				tripBooking.setDistanceInKm(distanceInKm);
				//calculating the bill
				int rate =ele.getCab().getPerKmRate();
				int distanceTravelled=distanceInKm;
				int totalPrice=rate*distanceTravelled;
				tripBooking.setBill(totalPrice);
				//set cab available to false of the coresponding driver
				ele.getCab().setAvailable(false);
				tripBooking.setDriver(ele);
				tripBooking.setCustomer(customer);
				tripBooking.setStatus(TripStatus.CONFIRMED);

				//since tripbooking is child of both driver and customer
				//first save it in the repo of tripbooking then, save it to parent

				TripBooking savedTripBooking=tripBookingRepository2.save(tripBooking);
				ele.getTripBookingList().add(savedTripBooking);
				driverRepository2.save(ele);
				customer.getTripBookingList().add(savedTripBooking);
				customerRepository2.save(customer);

				driverFound=true;

				return savedTripBooking;
			}

		}
		throw new Exception("No cab available!");

	}

	@Override
	public void cancelTrip(Integer tripId){
		//Cancel the trip having given trip Id and update TripBooking attributes accordingly
		Optional<TripBooking> optionalTripBooking=tripBookingRepository2.findById(tripId);
		TripBooking tripBooking=optionalTripBooking.get();
		//making trip status cancelled
		tripBooking.setStatus(TripStatus.CANCELED);
		//making the cab available
		tripBooking.getDriver().getCab().setAvailable(true);
		//bill ist set to 0;
		tripBooking.setBill(0);
		tripBookingRepository2.save(tripBooking);
	}

	@Override
	public void completeTrip(Integer tripId){
		//Complete the trip having given trip Id and update TripBooking attributes accordingly
		Optional<TripBooking> optionalTripBooking=tripBookingRepository2.findById(tripId);
		TripBooking tripBooking=optionalTripBooking.get();
		//confirming status
		tripBooking.setStatus(TripStatus.COMPLETED);
		//calculating total bill
		int ratePerKm=tripBooking.getDriver().getCab().getPerKmRate();
		int distanceTravelled=tripBooking.getDistanceInKm();
		int totalPrice=ratePerKm*distanceTravelled;
		tripBooking.setBill(totalPrice);
		//making the cab available for use
		tripBooking.getDriver().getCab().setAvailable(true);


		tripBookingRepository2.save(tripBooking);


	}
}
