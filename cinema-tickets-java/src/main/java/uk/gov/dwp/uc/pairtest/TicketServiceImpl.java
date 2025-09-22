package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.*;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */
    private static final int MAX_TICKETS = 25;
    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    private record TicketQuantities(int totalTickets, int adultTickets, int childTickets, int infantTickets) {
        int totalCost() {
            return (adultTickets * ADULT.getPrice()) + (childTickets * CHILD.getPrice()) + (infantTickets * INFANT.getPrice());
        }

        int totalSeats() {
            return adultTickets + childTickets;
        }
    }

    public TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        TicketQuantities quantities = validatePurchaseRequest(accountId, ticketTypeRequests);

        ticketPaymentService.makePayment(accountId, quantities.totalCost());
        seatReservationService.reserveSeat(accountId, quantities.totalSeats());
    }

    private TicketQuantities validatePurchaseRequest(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("At least one ticket must be requested for purchase");
        }

        for (TicketTypeRequest request : ticketTypeRequests) {
            if (request == null) {
                throw new InvalidPurchaseException("Ticket request cannot be null");
            }
        }

        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Invalid account ID");
        }

        var quantities = getTicketQuantities(ticketTypeRequests);

        if (quantities.totalTickets > MAX_TICKETS) {
            throw new InvalidPurchaseException("Maximum number of tickets exceeded (" + MAX_TICKETS + ")");
        }

        if ((quantities.childTickets > 0 || quantities.infantTickets > 0) && quantities.adultTickets == 0) {
            throw new InvalidPurchaseException("Child and Infant tickets must be purchased with an Adult ticket");
        }

        if (quantities.infantTickets > quantities.adultTickets) {
            throw new InvalidPurchaseException("Each Infant must be accompanied by an Adult");
        }

        return quantities;
    }

    private TicketQuantities getTicketQuantities(TicketTypeRequest... ticketTypeRequests) {
        int total = 0;
        int adultTickets = 0;
        int childTickets = 0;
        int infantTickets = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            var type = request.getTicketType();
            if (type == null) {
                throw new InvalidPurchaseException("Ticket type cannot be null");
            }
            int numberOfTickets = request.getNoOfTickets();
            total += numberOfTickets;
            switch (request.getTicketType()){
                case ADULT -> adultTickets += numberOfTickets;
                case CHILD -> childTickets += numberOfTickets;
                case INFANT -> infantTickets += numberOfTickets;
                default -> throw new InvalidPurchaseException("Unknown ticket type: " + request.getTicketType());
            }
        }

        return new TicketQuantities(total, adultTickets, childTickets, infantTickets);
    }

}
