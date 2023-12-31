import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { DataService } from '../../data.service';
import { InteracValidateRequest } from '../../dto/InteracValidateRequest';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-interac',
  templateUrl: './interac.component.html',
  styleUrls: ['./interac.component.scss']
})
export class InteracComponent implements OnInit, OnDestroy {
  interacForm!: FormGroup;
  interacFirstName!: string;
  interacLastName!: string;
  interacBankName!: string;
  interacEmailError!: string;
  intracBalance!: string;
  notLinked = false;
  amountEnabled = false;
  submitted = false;
  isBtnDisabled=true;
  isEmailValid=false;
  isUpiPayment=false;
  private readonly destroy$: Subject<void> = new Subject();

  constructor(
    private formBuilder: FormBuilder,
    private dataService: DataService,
    private http: HttpClient,
    private router: Router
  ) {
  }

  ngOnInit(): void {
    this.interacEmailError = '';
    this.interacForm = this.formBuilder.group({
      receiverEmail: ['', [Validators.required, Validators.email]],
      amount: ['', Validators.required],
      message: [''],
      securityQuestion: ['', Validators.required],
      securityAnswer: ['', Validators.required]
    });
    this.interacForm.get('amount')?.disable();
    this.interacForm.get('securityQuestion')?.disable();
    this.interacForm.get('securityAnswer')?.disable();
    this.interacForm.get('securityAnswer')?.disable();
    this.onchangeUsername();
    this.enableSubmit();
    console.log("................................",this.dataService.getTPartyIntracEmail)
    if (this.dataService.getTPartyIntracEmail().length>0){
      this.isUpiPayment = true;
    this.interacForm.patchValue({
  receiverEmail: this.dataService.getTPartyIntracEmail()
});
      this.interacForm.get('receiverEmail')?.disable();
    }
  this.onchangeUsername()

  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSubmit() {
    if (!localStorage.getItem('sessionId') && !this.dataService.isLoginValid) {
      this.router.navigate(['login']);
      return;
    }
    if (this.interacForm.invalid) {
      return;
    }
    this.submitted = true;
    this.interacForm.get('receiverEmail')?.enable();
    this.interacForm.value['sessionId'] = localStorage.getItem('sessionId') as string;
    this.dataService.sendInteracDetails(this.interacForm.value).subscribe((data) => {
      if (data.success) {
        this.interacForm.get('receiverEmail')?.disable();
        if(this.isUpiPayment) {
          alert("UPI Payment Successful");
        } else{
          alert(data.message);
        }
        this.dataService.updateSession(true, data.sessionId);
        this.router.navigate(['user-account'])
      }
    });
  }

  enableSubmit(){
	  if(this.isEmailValid&&this.interacForm.value['amount']&&this.interacForm.value['message']){
		  this.isBtnDisabled=false;
	  }
  }
  onchangeUsername(){
	   of(null).pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((email) => {
        if (this.interacForm.get('receiverEmail')?.value && this.validateEmail(this.interacForm.get('receiverEmail')?.value)) {
		this.isEmailValid = true;
          const interacValidateRequest: InteracValidateRequest = {
            sessionId: localStorage.getItem('sessionId') as string,
            email: this.interacForm.get('receiverEmail')?.value
          };
          return this.dataService.validateInterac(interacValidateRequest);
        } else {
          this.interacForm.get('amount')?.disable();
          this.amountEnabled = false;
          return [];
        }
      }),
      takeUntil(this.destroy$)
    )
      .subscribe((data) => {
        if (data.valid) {
          this.amountEnabled = true;
          this.interacForm.get('amount')?.enable();
          this.interacFirstName = data.firstName;
          this.interacLastName = data.lastName;
          this.interacBankName = data.bankName;
	  this.intracBalance = data.accountBalance;
          this.dataService.updateSession(true, data.sessionId);
        } else {
          this.notLinked = true;
          this.interacEmailError = 'Email not linked to any bank account';
          if (data.message === 'Cannot send to Self Account') {
            this.interacEmailError = 'Cannot send to Self Account';
          }
          this.amountEnabled = false;
          this.dataService.updateSession(true, data.sessionId);
          this.interacForm.get('amount')?.disable();
        }
      });
  }

  validateEmail(email: string): boolean {
    const emailRegex = /^([a-zA-Z0-9._%+-]+)@([a-zA-Z]{5,10})\.([a-zA-Z]{3})$/;
    return emailRegex.test(email);
  }
   validateAmount(): boolean {
	   if (Number(this.intracBalance) > Number(this.interacForm.get('amount')?.value)){
		   return true;
	   }
	     if(this.interacForm.get('amount')?.disabled){
            return true;
    }

    return false;
  }

}








