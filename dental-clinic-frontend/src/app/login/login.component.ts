import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from './auth.service';
import { SessionService, UserRole, workspaceFor } from '../core/auth/session.service';
import { LanguageSwitcherComponent } from '../core/i18n/language-switcher.component';

@Component({
  selector: 'app-login',
  imports: [FormsModule, CommonModule, LanguageSwitcherComponent, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent implements OnInit {
  username = '';
  password = '';

  isLoading = false;
  isSuccess = false;
  shakeCard = false;
  usernameError = '';
  passwordError = '';
  loginError = '';
  private readonly cdr = inject(ChangeDetectorRef);
  usernameFocused = false;
  passwordFocused = false;

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly session: SessionService,
  ) {}

  ngOnInit(): void {
    const user = this.session.currentUser;
    if (this.session.isAuthenticated && user) {
      void this.router.navigateByUrl(this.destinationFor(user.role), { replaceUrl: true });
    }
  }

  login(): void {
    this.usernameError = this.username.trim() ? '' : 'Username is required.';
    this.passwordError = this.password.trim() ? '' : 'Password is required.';
    this.loginError = '';
    this.isSuccess = false;

    if (this.usernameError || this.passwordError || this.isLoading) {
      this.triggerShake();
      return;
    }

    this.isLoading = true;

    this.authService
      .login({
        username: this.username.trim(),
        password: this.password,
      })
      .subscribe({
        next: (response) => {
          this.isLoading = false;
          this.isSuccess = true;
          this.cdr.markForCheck();

          void this.router.navigateByUrl(this.destinationFor(response.role), { replaceUrl: true });
        },

        error: (error: HttpErrorResponse) => {
          this.isLoading = false;
          this.isSuccess = false;
          this.loginError = this.getLoginErrorMessage(error);
          this.triggerShake();
          this.cdr.markForCheck();
        },
      });
  }

  private getLoginErrorMessage(error: HttpErrorResponse): string {
    const messages = error.error?.messages;
    if (Array.isArray(messages) && messages.length) {
      return messages.join(' ');
    }
    if (typeof error.error?.message === 'string') {
      return error.error.message;
    }
    if (error.status === 429) {
      return 'Too many failed attempts. Please wait a few minutes before trying again.';
    }

    if (error.status === 0) {
      return 'Login service is unavailable. Start the backend API and try again.';
    }

    return error.status === 401
      ? 'Invalid username or password.'
      : 'Login service is unavailable.';
  }

  private destinationFor(role: UserRole): string {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    const workspace = workspaceFor(role);
    return returnUrl && (returnUrl === workspace || returnUrl.startsWith(`${workspace}/`))
      ? returnUrl
      : this.authService.getRedirectUrl(role);
  }

  private triggerShake(): void {
    this.shakeCard = false;
    setTimeout(() => {
      this.shakeCard = true;
      this.cdr.markForCheck();
    });
  }
}
