package uk.gov.dwp.uc.pairtest;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;

import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.*;

@Slf4j
class TicketServiceIntegrationTest {

    static class TicketPaymentServiceStub implements TicketPaymentService {

        @Override
        public void makePayment(long accountId, int totalAmountToPay) {
            log.info("Payment made: account={}, cost={}", accountId, totalAmountToPay);
        }
    }

    static class SeatReservationServiceStub implements SeatReservationService {

        @Override
        public void reserveSeat(long accountId, int totalSeatsToAllocate) {
            log.info("Seats reserved: account={}, seats={}", accountId, totalSeatsToAllocate);
        }
    }

    @Test
    @Tag("Integration")
    void integrationTest_shouldProcessPaymentRequest() {
        var ticketPaymentService = new TicketPaymentServiceStub();
        var seatReservationService = new SeatReservationServiceStub();
        var ticketService = new TicketServiceImpl(ticketPaymentService, seatReservationService);

        var requests = new TicketTypeRequest[] {
                new TicketTypeRequest(ADULT, 2),
                new TicketTypeRequest(CHILD, 2),
                new TicketTypeRequest(INFANT, 1)
        };

        ticketService.purchaseTickets(1L, requests);
    }

}